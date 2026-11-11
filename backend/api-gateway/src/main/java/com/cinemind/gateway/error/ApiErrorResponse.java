package com.cinemind.gateway.error;

/** JSON body returned for gateway errors. Kept separate from downstream DTOs. */
public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path) {
}
