package com.epam.trainerworkload.messaging;

import com.epam.trainerworkload.ServiceTokens;
import com.epam.trainerworkload.model.ActionType;
import com.epam.trainerworkload.security.SecurityConfig;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkloadMessageReaderTest {

    private static final ValidatorFactory VALIDATION = Validation.buildDefaultValidatorFactory();

    private final WorkloadMessageReader reader = new WorkloadMessageReader(
            Jackson2ObjectMapperBuilder.json().build(),
            VALIDATION.getValidator(),
            new SecurityConfig().jwtDecoder(ServiceTokens.SECRET, "gym-crm", "trainer-workload-service"));

    @AfterAll
    static void closeValidation() {
        VALIDATION.close();
    }

    @Test
    void validMessage_isRead() {
        // when
        TrainerWorkloadMessage message = reader.read(json("\"Nodira.Karimova\"", "60", "\"ADD\""), ServiceTokens.valid());

        // then
        assertEquals(new TrainerWorkloadMessage("Nodira.Karimova", "Nodira", "Karimova", true,
                LocalDate.of(2026, 11, 10), 60, ActionType.ADD), message);
    }

    @Test
    void missingToken_isRejected() {
        assertRejected(json("\"Nodira.Karimova\"", "60", "\"ADD\""), null, "Missing service token");
    }

    @Test
    void tokenWithoutBearerPrefix_isRejected() {
        assertRejected(json("\"Nodira.Karimova\"", "60", "\"ADD\""), "Basic abc", "Missing service token");
    }

    @Test
    void tokenSignedWithOtherKey_isRejected() {
        String foreign = ServiceTokens.bearer(ServiceTokens.OTHER_SECRET, "gym-crm", "trainer-workload-service",
                Duration.ofMinutes(5));
        assertRejected(json("\"Nodira.Karimova\"", "60", "\"ADD\""), foreign, "Invalid service token");
    }

    @Test
    void emptyBody_isRejected() {
        assertRejected(" ", ServiceTokens.valid(), "Empty message body");
    }

    @Test
    void malformedJson_isRejected() {
        InvalidWorkloadMessageException exception = assertThrows(InvalidWorkloadMessageException.class,
                () -> reader.read("{not json", ServiceTokens.valid()));
        assertEquals(true, exception.getMessage().startsWith("Malformed JSON body"));
    }

    @Test
    void unknownActionType_isRejectedAsMalformed() {
        InvalidWorkloadMessageException exception = assertThrows(InvalidWorkloadMessageException.class,
                () -> reader.read(json("\"Nodira.Karimova\"", "60", "\"CANCEL\""), ServiceTokens.valid()));
        assertEquals(true, exception.getMessage().startsWith("Malformed JSON body"));
    }

    @Test
    void missingRequiredFields_areNamed() {
        assertRejected(json("null", "-5", "null"), ServiceTokens.valid(),
                "Invalid fields: actionType must not be null, trainerUsername must not be blank, "
                        + "trainingDuration must be greater than 0");
    }

    private void assertRejected(String payload, String authorization, String reason) {
        InvalidWorkloadMessageException exception = assertThrows(InvalidWorkloadMessageException.class,
                () -> reader.read(payload, authorization));
        assertEquals(reason, exception.getMessage());
    }

    private static String json(String username, String duration, String action) {
        return "{\"trainerUsername\":" + username + ",\"trainerFirstName\":\"Nodira\",\"trainerLastName\":\"Karimova\","
                + "\"isActive\":true,\"trainingDate\":\"2026-11-10\",\"trainingDuration\":" + duration
                + ",\"actionType\":" + action + "}";
    }
}
