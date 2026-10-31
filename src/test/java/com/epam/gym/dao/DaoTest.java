package com.epam.gym.dao;

import com.epam.gym.dao.impl.TraineeDaoImpl;
import com.epam.gym.dao.impl.TrainerDaoImpl;
import com.epam.gym.dao.impl.TrainingDaoImpl;
import com.epam.gym.dao.impl.TrainingTypeDaoImpl;
import com.epam.gym.dao.impl.UserDaoImpl;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({UserDaoImpl.class, TraineeDaoImpl.class, TrainerDaoImpl.class, TrainingDaoImpl.class,
        TrainingTypeDaoImpl.class})
public @interface DaoTest {
}
