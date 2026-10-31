package com.epam.gym.service.impl;

import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.Credentials;
import com.epam.gym.exception.AuthenticationFailedException;
import com.epam.gym.model.TrainingType;
import com.epam.gym.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypeServiceImplTest {

    private static final Credentials LOGIN = new Credentials("John.Smith", "password12");

    @Mock
    private TrainingTypeDao trainingTypeDao;

    @Mock
    private AuthenticationService authenticationService;

    private TrainingTypeServiceImpl trainingTypeService;

    @BeforeEach
    void setUp() {
        trainingTypeService = new TrainingTypeServiceImpl(trainingTypeDao, authenticationService);
    }

    @Test
    void findAll_authenticatedUser_returnsTypes() {
        // given
        List<TrainingType> types = List.of(new TrainingType("Yoga"));
        when(trainingTypeDao.findAll()).thenReturn(types);

        // when
        List<TrainingType> result = trainingTypeService.findAll(LOGIN);

        // then
        assertEquals(types, result);
        verify(authenticationService).authenticateUser(LOGIN);
    }

    @Test
    void findAll_failedAuthentication_doesNotQuery() {
        // given
        when(authenticationService.authenticateUser(LOGIN)).thenThrow(new AuthenticationFailedException("bad"));

        // when
        assertThrows(AuthenticationFailedException.class, () -> trainingTypeService.findAll(LOGIN));

        // then
        verify(trainingTypeDao, never()).findAll();
    }
}
