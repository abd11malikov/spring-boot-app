package com.epam.gym.service.impl;

import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dto.Credentials;
import com.epam.gym.monitoring.metrics.GymMetrics;
import com.epam.gym.model.Trainee;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.User;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.epam.gym.TestEntities.trainee;
import static com.epam.gym.TestEntities.trainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceImplTest {

    private static final String USERNAME = "John.Smith";

    @Mock
    private TraineeDao traineeDao;

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private TraineeServiceImpl traineeService;

    private Trainee john;

    @BeforeEach
    void setUp() {
        traineeService = new TraineeServiceImpl(
                traineeDao, trainerDao, usernameGenerator, passwordGenerator, passwordEncoder,
                new GymMetrics(meterRegistry));
        john = trainee("John", "Smith", "John.Smith");
    }

    @Test
    void create_setsGeneratedUsernameHashedPasswordAndActive() {
        // given
        Trainee trainee = new Trainee(new User("John", "Smith"));
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("aB3dE5gH7j");
        when(traineeDao.create(trainee)).thenReturn(trainee);

        // when
        Credentials credentials = traineeService.create(trainee);

        // then
        assertEquals(new Credentials("John.Smith", "aB3dE5gH7j"), credentials);
        assertEquals("John.Smith", trainee.getUser().getUsername());
        assertNotEquals("aB3dE5gH7j", trainee.getUser().getPassword());
        assertTrue(passwordEncoder.matches("aB3dE5gH7j", trainee.getUser().getPassword()));
        assertTrue(trainee.getUser().isActive());
        verify(traineeDao).create(trainee);
        assertEquals(1.0, meterRegistry.get("gym.registrations").tag("role", "trainee").counter().count());
    }

    @Test
    void create_missingFirstName_throwsExceptionAndSavesNothing() {
        // given
        Trainee trainee = new Trainee(new User(" ", "Smith"));

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> traineeService.create(trainee));

        // then
        assertEquals("First name is required", exception.getMessage());
        verify(traineeDao, never()).create(any());
        assertEquals(0.0, meterRegistry.get("gym.registrations").tag("role", "trainee").counter().count());
    }

    @Test
    void selectByUsername_existingTrainee_returnsIt() {
        // given
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(john));

        // when
        Trainee result = traineeService.selectByUsername("John.Smith");

        // then
        assertSame(john, result);
    }

    @Test
    void selectByUsername_unknownTrainee_throwsException() {
        // given
        when(traineeDao.findByUsername("Nobody.Here")).thenReturn(Optional.empty());

        // when
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> traineeService.selectByUsername("Nobody.Here"));

        // then
        assertEquals("Trainee Nobody.Here not found", exception.getMessage());
    }

    @Test
    void updateProfile_copiesFieldsIncludingActiveAndKeepsCredentials() {
        // given
        User changedUser = new User("Johnny", "Smith");
        changedUser.setActive(false);
        Trainee changes = new Trainee(changedUser);
        changes.setAddress("Samarkand");
        changes.setDateOfBirth(LocalDate.of(1999, 5, 5));
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(john));

        // when
        Trainee updated = traineeService.updateProfile(USERNAME, changes);

        // then
        assertSame(john, updated);
        assertEquals("Johnny", updated.getUser().getFirstName());
        assertEquals("Samarkand", updated.getAddress());
        assertEquals(LocalDate.of(1999, 5, 5), updated.getDateOfBirth());
        assertFalse(updated.getUser().isActive());
        assertEquals("John.Smith", updated.getUser().getUsername());
        assertEquals("password12", updated.getUser().getPassword());
    }

    @Test
    void updateProfile_missingLastName_throwsException() {
        // given
        Trainee changes = new Trainee(new User("John", null));

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> traineeService.updateProfile(USERNAME, changes));

        // then
        assertEquals("Last name is required", exception.getMessage());
    }

    @Test
    void setActive_false_deactivatesActiveTrainee() {
        // given
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(john));

        // when
        traineeService.setActive(USERNAME, false);

        // then
        assertFalse(john.getUser().isActive());
    }

    @Test
    void setActive_true_activatesInactiveTrainee() {
        // given
        john.getUser().setActive(false);
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(john));

        // when
        traineeService.setActive(USERNAME, true);

        // then
        assertTrue(john.getUser().isActive());
    }

    @Test
    void setActive_sameStatus_throwsException() {
        // given
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(john));

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> traineeService.setActive(USERNAME, true));

        // then
        assertEquals("Trainee John.Smith is already active", exception.getMessage());
    }

    @Test
    void delete_authenticatedTrainee_deletesIt() {
        // given
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(john));

        // when
        traineeService.delete(USERNAME);

        // then
        verify(traineeDao).delete(john);
    }

    @Test
    void delete_userIsNotATrainee_deletesNothing() {
        // given
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.empty());

        // when
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> traineeService.delete(USERNAME));

        // then
        assertEquals("Trainee John.Smith not found", exception.getMessage());
        verify(traineeDao, never()).delete(any());
    }

    @Test
    void getUnassignedTrainers_returnsActiveTrainersFromDao() {
        // given
        List<Trainer> trainers = List.of(trainer("Nodira", "Karimova", "Nodira.Karimova", new TrainingType("Yoga")));
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(john));
        when(trainerDao.findActiveNotAssignedToTrainee("John.Smith")).thenReturn(trainers);

        // when
        List<Trainer> result = traineeService.getUnassignedTrainers(USERNAME);

        // then
        assertEquals(trainers, result);
    }

    @Test
    void updateTrainers_replacesTraineeTrainers() {
        // given
        Trainer oldTrainer = trainer("Old", "Trainer", "Old.Trainer", new TrainingType("Yoga"));
        Trainer nodira = trainer("Nodira", "Karimova", "Nodira.Karimova", new TrainingType("Yoga"));
        john.getTrainers().add(oldTrainer);
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(john));
        when(trainerDao.findByUsername("Nodira.Karimova")).thenReturn(Optional.of(nodira));

        // when
        List<Trainer> result = traineeService.updateTrainers(USERNAME, List.of("Nodira.Karimova"));

        // then
        assertEquals(List.of(nodira), result);
        assertEquals(1, john.getTrainers().size());
        assertTrue(john.getTrainers().contains(nodira));
    }

    @Test
    void updateTrainers_unknownTrainer_throwsExceptionAndKeepsOldList() {
        // given
        Trainer oldTrainer = trainer("Old", "Trainer", "Old.Trainer", new TrainingType("Yoga"));
        john.getTrainers().add(oldTrainer);
        when(traineeDao.findByUsername(USERNAME)).thenReturn(Optional.of(john));
        when(trainerDao.findByUsername("Nobody.Here")).thenReturn(Optional.empty());

        // when
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> traineeService.updateTrainers(USERNAME, List.of("Nobody.Here")));

        // then
        assertEquals("Trainer Nobody.Here not found", exception.getMessage());
        assertTrue(john.getTrainers().contains(oldTrainer));
    }
}
