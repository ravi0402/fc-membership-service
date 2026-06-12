package com.firstclub.membership.common.api;

/**
 * Uniform response envelope for every endpoint.
 *
 * <p>Immutable {@code record}: exactly one of {@code data} / {@code error} is meaningful
 * depending on {@code success}.
 *
 * @param <T> payload type
 */
public record ApiResponse<T>(boolean success, T data, String error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> failure(String error) {
        return new ApiResponse<>(false, null, error);
    }
}
