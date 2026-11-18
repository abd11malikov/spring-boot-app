package com.epam.gym.service.impl;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.Credentials;
import com.epam.gym.monitoring.metrics.GymMetrics;
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

import java.util.NoSuchElementException;
import java.util.Optional;

import static com.epam.gym.TestEntities.trainer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceImplTest {

    private static final String USERNAME = "Nodira.Karimova";

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private TrainingTypeDao trainingTypeDao;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private TrainerServiceImpl trainerService;

    private final TrainingType yoga = new TrainingType("Yoga");
    private Trainer nodira;

    @BeforeEach
    void setUp() {
        trainerService = new TrainerServiceImpl(
                trainerDao, trainingTypeDao, usernameGenerator, passwordGenerator, passwordEncoder,
                new GymMetrics(meterRegistry));
        nodira = trainer("Nodira", "Karimova", "Nodira.Karimova", yoga);
    }

    @Test
    void create_setsCredentialsAndSpecializationLoadedById() {
        // given
        Trainer trainer = new Trainer();
        trainer.setUser(new User("Nodira", "Karimova"));
        when(trainingTypeDao.findById(2L)).thenReturn(Optional.of(yoga));
        when(usernameGenerator.generate("Nodira", "Karimova")).thenReturn("Nodira.Karimova");
        when(passwordGenerator.generate()).thenReturn("sT7uV1wX3y");
        when(trainerDao.create(trainer)).thenReturn(trainer);

        // when
        Credentials credentials = trainerService.create(trainer, 2L);

        // then
        assertEquals(new Credentials("Nodira.Karimova", "sT7uV1wX3y"), credentials);
        assertSame(yoga, trainer.getSpecialization());
        assertEquals("Nodira.Karimova", trainer.getUser().getUsername());
        assertTrue(passwordEncoder.matches("sT7uV1wX3y", trainer.getUser().getPassword()));
        assertTrue(trainer.getUser().isActive());
        assertEquals(1.0, meterRegistry.get("gym.registrations").tag("role", "trainer").counter().count());
    }

    @Test
    void create_unknownSpecialization_throwsException() {
        // given
        Trainer trainer = new Trainer();
        trainer.setUser(new User("Nodira", "Karimova"));
        when(trainingTypeDao.findById(99L)).thenReturn(Optional.empty());

        // when
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> trainerService.create(trainer, 99L));

        // then
        assertEquals("Training type with id 99 not found", exception.getMessage());
        verify(trainerDao, never()).create(any());
    }

    @Test
    void create_withoutSpecialization_throwsException() {
        // given
        Trainer trainer = new Trainer();
        trainer.setUser(new User("Nodira", "Karimova"));

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> trainerService.create(trainer, null));

        // then
        assertEquals("Specialization is required", exception.getMessage());
        verify(trainerDao, never()).create(any());
    }

    @Test
    void create_missingLastName_throwsException() {
        // given
        Trainer trainer = new Trainer();
        trainer.setUser(new User("Nodira", ""));

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> trainerService.create(trainer, 2L));

        // then
        assertEquals("Last name is required", exception.getMessage());
    }

    @Test
    void selectByUsername_existingTrainer_returnsIt() {
        // given
        when(trainerDao.findByUsername("Nodira.Karimova")).thenReturn(Optional.of(nodira));

        // when
        Trainer result = trainerService.selectByUsername("Nodira.Karimova");

        // then
        assertSame(nodira, result);
    }

    @Test
    void selectByUsername_unknownTrainer_throwsException() {
        // given
        when(trainerDao.findByUsername("Nobody.Here")).thenReturn(Optional.empty());

        // when
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> trainerService.selectByUsername("Nobody.Here"));

        // then
        assertEquals("Trainer Nobody.Here not found", exception.getMessage());
    }

    @Test
    void updateProfile_copiesNamesAndActiveButKeepsSpecialization() {
        // given
        User changedUser = new User("Nodira", "Aliyeva");
        changedUser.setActive(false);
        Trainer changes = new Trainer(changedUser, new TrainingType("Cardio"));
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(nodira));

        // when
        Trainer updated = trainerService.updateProfile(USERNAME, changes);

        // then
        assertSame(nodira, updated);
        assertEquals("Aliyeva", updated.getUser().getLastName());
        assertFalse(updated.getUser().isActive());
        assertSame(yoga, updated.getSpecialization());
        assertEquals("Nodira.Karimova", updated.getUser().getUsername());
        assertEquals("password12", updated.getUser().getPassword());
    }

    @Test
    void setActive_false_deactivatesActiveTrainer() {
        // given
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(nodira));

        // when
        trainerService.setActive(USERNAME, false);

        // then
        assertFalse(nodira.getUser().isActive());
    }

    @Test
    void setActive_true_activatesInactiveTrainer() {
        // given
        nodira.getUser().setActive(false);
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(nodira));

        // when
        trainerService.setActive(USERNAME, true);

        // then
        assertTrue(nodira.getUser().isActive());
    }

    @Test
    void setActive_sameStatus_throwsException() {
        // given
        nodira.getUser().setActive(false);
        when(trainerDao.findByUsername(USERNAME)).thenReturn(Optional.of(nodira));

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> trainerService.setActive(USERNAME, false));

        // then
        assertEquals("Trainer Nodira.Karimova is already inactive", exception.getMessage());
    }
}
