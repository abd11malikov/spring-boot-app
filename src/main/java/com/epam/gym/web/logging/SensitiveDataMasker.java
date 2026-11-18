package com.epam.gym.web.logging;

import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    private static final Pattern PASSWORD_FIELD =
            Pattern.compile("(\"(?:password|oldPassword|newPassword|token)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*(?:\"|$)");
    private static final String MASK = "\"****\"";

    private SensitiveDataMasker() {
    }

    public static String mask(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        return PASSWORD_FIELD.matcher(json).replaceAll("$1" + MASK);
    }
}
