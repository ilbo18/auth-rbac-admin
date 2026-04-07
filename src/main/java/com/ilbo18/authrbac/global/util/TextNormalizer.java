package com.ilbo18.authrbac.global.util;

import lombok.experimental.UtilityClass;

import java.util.Locale;

/**
 * 문자열 정규화 공용 유틸
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    /** 양끝 공백 제거 후 소문자로 정규화한다. null이면 빈 문자열을 반환 */
    public static String trimToLowerCase(String value) {
        if (value == null) return "";

        return value.trim().toLowerCase(Locale.ROOT);
    }
}
