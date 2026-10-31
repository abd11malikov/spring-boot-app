package com.epam.gym.web.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SensitiveDataMaskerTest {

    @Test
    void mask_passwordField_isReplaced() {
        // given
        String json = "{\"username\":\"John.Doe\",\"password\":\"aB3dE5gH7j\"}";

        // when
        String masked = SensitiveDataMasker.mask(json);

        // then
        assertEquals("{\"username\":\"John.Doe\",\"password\":\"****\"}", masked);
    }

    @Test
    void mask_oldAndNewPassword_areBothReplaced() {
        // given
        String json = "{\"username\":\"John.Doe\",\"oldPassword\":\"old1\", \"newPassword\" : \"new2\"}";

        // when
        String masked = SensitiveDataMasker.mask(json);

        // then
        assertEquals("{\"username\":\"John.Doe\",\"oldPassword\":\"****\", \"newPassword\" : \"****\"}", masked);
    }

    @Test
    void mask_passwordWithEscapedQuote_isFullyReplaced() {
        // given
        String json = "{\"password\":\"ab\\\"cd\",\"next\":1}";

        // when
        String masked = SensitiveDataMasker.mask(json);

        // then
        assertEquals("{\"password\":\"****\",\"next\":1}", masked);
    }

    @Test
    void mask_cutOffPasswordValue_isStillReplaced() {
        // given
        String json = "{\"username\":\"John.Doe\",\"password\":\"aB3dE";

        // when
        String masked = SensitiveDataMasker.mask(json);

        // then
        assertEquals("{\"username\":\"John.Doe\",\"password\":\"****\"", masked);
    }

    @Test
    void mask_bodyWithoutPasswords_isUnchanged() {
        // given
        String json = "{\"firstName\":\"John\",\"passwordHint\":\"pet name\"}";

        // when
        String masked = SensitiveDataMasker.mask(json);

        // then
        assertEquals(json, masked);
    }

    @Test
    void mask_null_returnsNull() {
        // given
        String json = null;

        // when
        String masked = SensitiveDataMasker.mask(json);

        // then
        assertNull(masked);
    }
}
