package com.epam.gym.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DaoTest
class DatabaseInitializationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void schemaScript_createsAllTables() {
        // given
        List<String> expectedTables =
                List.of("users", "trainees", "trainers", "training_types", "trainings", "trainee_trainer");

        // when
        List<String> existingTables = jdbcTemplate.queryForList(
                "SELECT LOWER(table_name) FROM information_schema.tables", String.class);

        // then
        assertTrue(existingTables.containsAll(expectedTables), "Existing tables: " + existingTables);
    }

    @Test
    void dataScript_insertsTrainingTypes() {
        // given
        String query = "SELECT training_type_name FROM training_types ORDER BY training_type_name";

        // when
        List<String> names = jdbcTemplate.queryForList(query, String.class);

        // then
        assertEquals(List.of("Cardio", "Fitness", "Resistance", "Stretching", "Yoga", "Zumba"), names);
    }
}
