package com.epam.gym.service.impl;

import com.epam.gym.dao.UserDao;
import com.epam.gym.exception.AuthenticationFailedException;
import com.epam.gym.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserDao userDao;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthenticationServiceImpl authenticationService;

    private User john;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(userDao, passwordEncoder);
        john = new User("John", "Smith");
        john.setUsername("John.Smith");
        john.setPassword(passwordEncoder.encode("password12"));
    }

    @Test
    void changePassword_rightOldPassword_storesHashOfNewPassword() {
        // given
        when(userDao.findByUsername("John.Smith")).thenReturn(Optional.of(john));

        // when
        authenticationService.changePassword("John.Smith", "password12", "newSecret1");

        // then
        assertNotEquals("newSecret1", john.getPassword());
        assertTrue(passwordEncoder.matches("newSecret1", john.getPassword()));
        assertFalse(passwordEncoder.matches("password12", john.getPassword()));
    }

    @Test
    void changePassword_wrongOldPassword_throwsAndKeepsPassword() {
        // given
        String oldHash = john.getPassword();
        when(userDao.findByUsername("John.Smith")).thenReturn(Optional.of(john));

        // when
        AuthenticationFailedException exception = assertThrows(AuthenticationFailedException.class,
                () -> authenticationService.changePassword("John.Smith", "wrong", "newSecret1"));

        // then
        assertEquals("Invalid username or password", exception.getMessage());
        assertEquals(oldHash, john.getPassword());
    }

    @Test
    void changePassword_unknownUser_throws() {
        // given
        when(userDao.findByUsername("Nobody.Here")).thenReturn(Optional.empty());

        // when / then
        assertThrows(AuthenticationFailedException.class,
                () -> authenticationService.changePassword("Nobody.Here", "password12", "newSecret1"));
    }

    @Test
    void changePassword_missingOldPassword_throws() {
        // when / then
        assertThrows(AuthenticationFailedException.class,
                () -> authenticationService.changePassword("John.Smith", null, "newSecret1"));
    }

    @Test
    void changePassword_blankNewPassword_throwsValidationError() {
        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authenticationService.changePassword("John.Smith", "password12", " "));

        // then
        assertEquals("New password is required", exception.getMessage());
    }
}
