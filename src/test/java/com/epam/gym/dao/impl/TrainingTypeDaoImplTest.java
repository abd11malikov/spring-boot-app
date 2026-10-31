package com.epam.gym.dao.impl;

import com.epam.gym.config.AppConfig;
import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.model.TrainingType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(AppConfig.class)
@TestPropertySource("classpath:application-test.properties")
@Transactional
class TrainingTypeDaoImplTest {

    @Autowired
    private TrainingTypeDao trainingTypeDao;

    @Test
    void findAll_returnsSeededTypesSortedByName() {
        // given
        int seededTypes = 6;

        // when
        List<TrainingType> types = trainingTypeDao.findAll();

        // then
        assertEquals(seededTypes, types.size());
        assertEquals("Cardio", types.get(0).getTrainingTypeName());
    }

    @Test
    void findByName_existingType_returnsIt() {
        // given
        String name = "Yoga";

        // when
        Optional<TrainingType> type = trainingTypeDao.findByName(name);

        // then
        assertTrue(type.isPresent());
        assertNotNull(type.get().getId());
        assertEquals(name, type.get().getTrainingTypeName());
    }

    @Test
    void findById_existingType_returnsIt() {
        // given
        Long yogaId = trainingTypeDao.findByName("Yoga").orElseThrow().getId();

        // when
        Optional<TrainingType> type = trainingTypeDao.findById(yogaId);

        // then
        assertTrue(type.isPresent());
        assertEquals("Yoga", type.get().getTrainingTypeName());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        // given
        Long unknownId = 9999L;

        // when
        Optional<TrainingType> type = trainingTypeDao.findById(unknownId);

        // then
        assertTrue(type.isEmpty());
    }

    @Test
    void findByName_unknownType_returnsEmpty() {
        // given
        String name = "Swimming";

        // when
        Optional<TrainingType> type = trainingTypeDao.findByName(name);

        // then
        assertTrue(type.isEmpty());
    }
}
