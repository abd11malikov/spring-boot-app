package com.epam.gym.web.dto;

import com.epam.gym.web.dto.request.ActivationRequest;
import com.epam.gym.web.dto.request.AddTrainingRequest;
import com.epam.gym.web.dto.request.ChangePasswordRequest;
import com.epam.gym.web.dto.request.TraineeRegistrationRequest;
import com.epam.gym.web.dto.request.TrainerRegistrationRequest;
import com.epam.gym.web.dto.request.UpdateTraineeRequest;
import com.epam.gym.web.dto.request.UpdateTraineeTrainersRequest;
import com.epam.gym.web.dto.request.UpdateTrainerRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private static final LocalDate PAST = LocalDate.of(1998, 3, 15);

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void traineeRegistration_onlyRequiredFields_isValid() {
        // given
        TraineeRegistrationRequest request = new TraineeRegistrationRequest("John", "Doe", null, null);

        // when
        Set<ConstraintViolation<TraineeRegistrationRequest>> violations = validator.validate(request);

        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    void traineeRegistration_blankNames_reportsBothFields() {
        // given
        TraineeRegistrationRequest request = new TraineeRegistrationRequest(" ", null, PAST, "Tashkent");

        // when
        Set<ConstraintViolation<TraineeRegistrationRequest>> violations = validator.validate(request);

        // then
        assertEquals(List.of("firstName", "lastName"), invalidFields(violations));
    }

    @Test
    void traineeRegistration_futureDateOfBirth_isInvalid() {
        // given
        TraineeRegistrationRequest request =
                new TraineeRegistrationRequest("John", "Doe", LocalDate.now().plusDays(1), null);

        // when
        Set<ConstraintViolation<TraineeRegistrationRequest>> violations = validator.validate(request);

        // then
        assertEquals(List.of("dateOfBirth"), invalidFields(violations));
    }

    @Test
    void trainerRegistration_missingSpecialization_isInvalid() {
        // given
        TrainerRegistrationRequest request = new TrainerRegistrationRequest("Nodira", "Karimova", null);

        // when
        Set<ConstraintViolation<TrainerRegistrationRequest>> violations = validator.validate(request);

        // then
        assertEquals(List.of("specializationId"), invalidFields(violations));
    }

    @Test
    void changePassword_blankFields_reportsAllThree() {
        // given
        ChangePasswordRequest request = new ChangePasswordRequest("", null, " ");

        // when
        Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);

        // then
        assertEquals(List.of("newPassword", "oldPassword", "username"), invalidFields(violations));
    }

    @Test
    void updateTrainee_missingIsActive_isInvalid() {
        // given
        UpdateTraineeRequest request = new UpdateTraineeRequest("John", "Doe", PAST, "Tashkent", null);

        // when
        Set<ConstraintViolation<UpdateTraineeRequest>> violations = validator.validate(request);

        // then
        assertEquals(List.of("isActive"), invalidFields(violations));
    }

    @Test
    void updateTrainer_tooLongFirstName_isInvalid() {
        // given
        UpdateTrainerRequest request = new UpdateTrainerRequest("N".repeat(101), "Karimova", true);

        // when
        Set<ConstraintViolation<UpdateTrainerRequest>> violations = validator.validate(request);

        // then
        assertEquals(List.of("firstName"), invalidFields(violations));
    }

    @Test
    void updateTraineeTrainers_emptyList_isValid() {
        // given
        UpdateTraineeTrainersRequest request = new UpdateTraineeTrainersRequest(List.of());

        // when
        Set<ConstraintViolation<UpdateTraineeTrainersRequest>> violations = validator.validate(request);

        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    void updateTraineeTrainers_blankUsernameInList_isInvalid() {
        // given
        UpdateTraineeTrainersRequest request = new UpdateTraineeTrainersRequest(Arrays.asList("Nodira.Karimova", " "));

        // when
        Set<ConstraintViolation<UpdateTraineeTrainersRequest>> violations = validator.validate(request);

        // then
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getPropertyPath().toString().startsWith("trainerUsernames["));
    }

    @Test
    void addTraining_zeroDurationAndMissingDate_reportsBoth() {
        // given
        AddTrainingRequest request =
                new AddTrainingRequest("John.Doe", "Nodira.Karimova", "Morning Yoga", null, 0);

        // when
        Set<ConstraintViolation<AddTrainingRequest>> violations = validator.validate(request);

        // then
        assertEquals(List.of("trainingDate", "trainingDuration"), invalidFields(violations));
    }

    @Test
    void activation_missingIsActive_isInvalid() {
        // given
        ActivationRequest request = new ActivationRequest(null);

        // when
        Set<ConstraintViolation<ActivationRequest>> violations = validator.validate(request);

        // then
        assertEquals(List.of("isActive"), invalidFields(violations));
    }

    private <T> List<String> invalidFields(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .sorted()
                .toList();
    }
}
