package com.ilbo18.authrbac.global.response;

/**
 * API 성공 응답 공통 구조
 * */
public record ApiResponseRecord<T>(int code, T data) {

    private static final int OK = 200;
    private static final String DEFAULT_SUCCESS_MESSAGE = "정상 처리되었습니다.";

    /** 데이터를 포함한 성공 응답 */
    public static <T> ApiResponseRecord<T> success(T data) {
        return new ApiResponseRecord<>(OK, data);
    }

    /** 데이터 없이 메시지만 반환하는 성공 응답 */
    public static ApiResponseRecord<String> success() {
        return new ApiResponseRecord<>(OK, DEFAULT_SUCCESS_MESSAGE);
    }

    /** 메시지와 데이터를 함께 반환하는 성공 응답 */
    public static <T> ApiResponseRecord<SuccessWithMessage<T>> successWithMessage(String message, T data) {
        return new ApiResponseRecord<>(OK, new SuccessWithMessage<>(message, data));
    }

    /** 성공 메시지 + 데이터 묶음 */
    public record SuccessWithMessage<T>(String message, T data) {
    }
}
