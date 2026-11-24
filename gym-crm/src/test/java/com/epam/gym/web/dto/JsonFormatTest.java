package com.epam.gym.web.dto;

import com.epam.gym.web.dto.request.UpdateTraineeRequest;
import com.epam.gym.web.dto.response.RegistrationResponse;
import com.epam.gym.web.dto.response.TokenResponse;
import com.epam.gym.web.dto.response.TraineeProfileResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JsonTest
class JsonFormatTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void profileResponse_writesIsActiveAndIsoDate() throws Exception {
        // given
        TraineeProfileResponse response = new TraineeProfileResponse(
                "John.Doe", "John", "Doe", LocalDate.of(1998, 3, 15), "Tashkent", true, List.of());

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        // then
        assertTrue(json.get("isActive").asBoolean());
        assertFalse(json.has("active"));
        assertEquals("1998-03-15", json.get("dateOfBirth").asText());
    }

    @Test
    void updateRequest_readsIsActiveAndIsoDate() throws Exception {
        // given
        String json = """
                {"firstName":"John","lastName":"Doe","dateOfBirth":"1998-03-15","address":"Tashkent","isActive":false}
                """;

        // when
        UpdateTraineeRequest request = objectMapper.readValue(json, UpdateTraineeRequest.class);

        // then
        assertEquals(Boolean.FALSE, request.isActive());
        assertEquals(LocalDate.of(1998, 3, 15), request.dateOfBirth());
    }

    @Test
    void updateRequest_ignoresUnknownFieldsSuchAsReadOnlySpecialization() throws Exception {
        // given
        String json = """
                {"firstName":"John","lastName":"Doe","isActive":true,"specializationId":5}
                """;

        // when
        UpdateTraineeRequest request = objectMapper.readValue(json, UpdateTraineeRequest.class);

        // then
        assertEquals("John", request.firstName());
    }

    @Test
    void registrationResponse_toString_hidesPasswordAndToken() {
        // given
        RegistrationResponse response = new RegistrationResponse("John.Doe", "aB3dE5gH7j", "header.payload.signature");

        // when
        String text = response.toString();

        // then
        assertFalse(text.contains("aB3dE5gH7j"));
        assertFalse(text.contains("header.payload.signature"));
    }

    @Test
    void tokenResponse_toString_hidesToken() {
        // given
        TokenResponse response = TokenResponse.bearer("header.payload.signature", 3600);

        // when
        String text = response.toString();

        // then
        assertFalse(text.contains("header.payload.signature"));
    }
}
