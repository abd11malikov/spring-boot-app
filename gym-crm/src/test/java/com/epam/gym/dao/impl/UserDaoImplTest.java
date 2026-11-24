package com.epam.gym.dao.impl;

import com.epam.gym.dao.DaoTest;
import com.epam.gym.dao.TraineeDao;
import com.epam.gym.dao.UserDao;
import com.epam.gym.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static com.epam.gym.TestEntities.trainee;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DaoTest
class UserDaoImplTest {

    @Autowired
    private UserDao userDao;

    @Autowired
    private TraineeDao traineeDao;

    @Test
    void existsByUsername_existingUser_returnsTrue() {
        // given
        traineeDao.create(trainee("John", "Smith", "John.Smith"));

        // when
        boolean exists = userDao.existsByUsername("John.Smith");

        // then
        assertTrue(exists);
    }

    @Test
    void existsByUsername_unknownUser_returnsFalse() {
        // given
        String username = "Nobody.Here";

        // when
        boolean exists = userDao.existsByUsername(username);

        // then
        assertFalse(exists);
    }

    @Test
    void findByUsername_existingUser_returnsIt() {
        // given
        traineeDao.create(trainee("John", "Smith", "John.Smith"));

        // when
        Optional<User> user = userDao.findByUsername("John.Smith");

        // then
        assertTrue(user.isPresent());
        assertEquals("John", user.get().getFirstName());
    }

    @Test
    void findByUsername_unknownUser_returnsEmpty() {
        // given
        String username = "Nobody.Here";

        // when
        Optional<User> user = userDao.findByUsername(username);

        // then
        assertTrue(user.isEmpty());
    }
}
