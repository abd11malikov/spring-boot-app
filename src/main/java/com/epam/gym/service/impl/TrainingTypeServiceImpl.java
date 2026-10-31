package com.epam.gym.service.impl;

import com.epam.gym.dao.TrainingTypeDao;
import com.epam.gym.dto.Credentials;
import com.epam.gym.model.TrainingType;
import com.epam.gym.service.AuthenticationService;
import com.epam.gym.service.TrainingTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TrainingTypeServiceImpl implements TrainingTypeService {

    private final TrainingTypeDao trainingTypeDao;
    private final AuthenticationService authenticationService;

    public TrainingTypeServiceImpl(TrainingTypeDao trainingTypeDao, AuthenticationService authenticationService) {
        this.trainingTypeDao = trainingTypeDao;
        this.authenticationService = authenticationService;
    }

    @Override
    public List<TrainingType> findAll(Credentials credentials) {
        authenticationService.authenticateUser(credentials);
        return trainingTypeDao.findAll();
    }
}
