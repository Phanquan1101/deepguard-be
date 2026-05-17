package com.deepguard.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @Builder.Default
    private boolean success = false;
    private String code;
    private String message;
    private LocalDateTime timestamp;
    private String path;
    private Object errors;

    public static ErrorResponse from(ErrorCode errorCode, String path) {
        return ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    public static ErrorResponse from(ErrorCode errorCode, String path, String message, Object errors) {
        return ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .errors(errors)
                .build();
    }
}
