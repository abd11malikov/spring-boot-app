package com.epam.gym.service.impl;

import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.model.TrainingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypeServiceImplTest {

    @Mock
    private TrainingTypeDao trainingTypeDao;

    @InjectMocks
    private TrainingTypeServiceImpl trainingTypeService;

    @Test
    void findAll_returnsTypesFromDao() {
        // given
        List<TrainingType> types = List.of(new TrainingType("Yoga"));
        when(trainingTypeDao.findAll()).thenReturn(types);

        // when
        List<TrainingType> result = trainingTypeService.findAll();

        // then
        assertEquals(types, result);
    }
}
