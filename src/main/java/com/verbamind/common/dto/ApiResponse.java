package com.verbamind.common.dto;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        Instant timestamp,
        String message
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, Instant.now(), null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, Instant.now(), message);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, null, Instant.now(), message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, Instant.now(), message);
    }

    public static <T> ApiResponse<T> error(T data, String message) {
        return new ApiResponse<>(false, data, Instant.now(), message);
    }
}