package com.epam.gym.service.impl;

import com.epam.gym.dao.TrainerDao;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.Credentials;
import com.epam.gym.model.Trainer;
import com.epam.gym.model.TrainingType;
import com.epam.gym.model.User;
import com.epam.gym.service.AuthenticationService;
import com.epam.gym.util.PasswordGenerator;
import com.epam.gym.util.UsernameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private static final Credentials LOGIN = new Credentials("Nodira.Karimova", "password12");

    @Mock
    private TrainerDao trainerDao;

    @Mock
    private TrainingTypeDao trainingTypeDao;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UsernameGenerator usernameGenerator;

    @Mock
    private PasswordGenerator passwordGenerator;

    private TrainerServiceImpl trainerService;

    private final TrainingType yoga = new TrainingType("Yoga");
    private Trainer nodira;

    @BeforeEach
    void setUp() {
        trainerService = new TrainerServiceImpl(
                trainerDao, trainingTypeDao, authenticationService, usernameGenerator, passwordGenerator);
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
        Trainer created = trainerService.create(trainer, 2L);

        // then
        assertSame(yoga, created.getSpecialization());
        assertEquals("Nodira.Karimova", created.getUser().getUsername());
        assertEquals("sT7uV1wX3y", created.getUser().getPassword());
        assertTrue(created.getUser().isActive());
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
    void selectByUsername_authenticatedUser_returnsTrainer() {
        // given
        when(trainerDao.findByUsername("Nodira.Karimova")).thenReturn(Optional.of(nodira));

        // when
        Trainer result = trainerService.selectByUsername(LOGIN, "Nodira.Karimova");

        // then
        assertSame(nodira, result);
        verify(authenticationService).authenticateUser(LOGIN);
    }

    @Test
    void selectByUsername_unknownTrainer_throwsException() {
        // given
        when(trainerDao.findByUsername("Nobody.Here")).thenReturn(Optional.empty());

        // when
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> trainerService.selectByUsername(LOGIN, "Nobody.Here"));

        // then
        assertEquals("Trainer Nobody.Here not found", exception.getMessage());
    }

    @Test
    void updateProfile_copiesNamesAndActiveButKeepsSpecialization() {
        // given
        User changedUser = new User("Nodira", "Aliyeva");
        changedUser.setActive(false);
        Trainer changes = new Trainer(changedUser, new TrainingType("Cardio"));
        when(authenticationService.authenticateTrainer(LOGIN)).thenReturn(nodira);

        // when
        Trainer updated = trainerService.updateProfile(LOGIN, changes);

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
        when(authenticationService.authenticateTrainer(LOGIN)).thenReturn(nodira);

        // when
        trainerService.setActive(LOGIN, false);

        // then
        assertFalse(nodira.getUser().isActive());
    }

    @Test
    void setActive_true_activatesInactiveTrainer() {
        // given
        nodira.getUser().setActive(false);
        when(authenticationService.authenticateTrainer(LOGIN)).thenReturn(nodira);

        // when
        trainerService.setActive(LOGIN, true);

        // then
        assertTrue(nodira.getUser().isActive());
    }

    @Test
    void setActive_sameStatus_throwsException() {
        // given
        nodira.getUser().setActive(false);
        when(authenticationService.authenticateTrainer(LOGIN)).thenReturn(nodira);

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> trainerService.setActive(LOGIN, false));

        // then
        assertEquals("Trainer Nodira.Karimova is already inactive", exception.getMessage());
    }
}
