package com.ilbo18.authrbac.global.util;

import com.ilbo18.authrbac.global.exception.CustomException;
import com.ilbo18.authrbac.global.exception.ErrorCode;
import java.util.Locale;

public final class EnumParser {

    private EnumParser() {
    }

    public static <T extends Enum<T>> T parseOrNull(String value, Class<T> enumType, ErrorCode errorCode) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CustomException(errorCode);
        }
    }
}
