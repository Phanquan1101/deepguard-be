package com.deepguard.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.deepguard.auth.entity.Role;
import com.deepguard.auth.entity.User;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),

    UNAUTHORIZED("UNAUTHORIZED", "Authentication is required", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("ACCESS_DENIED", "You do not have permission to access this resource", HttpStatus.FORBIDDEN),

    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "Email already exists", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS("USERNAME_ALREADY_EXISTS", "Username already exists", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid email/username or password", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_NOT_FOUND("REFRESH_TOKEN_NOT_FOUND", "Refresh token not found", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_EXPIRED("REFRESH_TOKEN_EXPIRED", "Refresh token has expired", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REVOKED("REFRESH_TOKEN_REVOKED", "Refresh token has been revoked", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "Account is disabled", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_VERIFIED("ACCOUNT_NOT_VERIFIED", "Account is not verified", HttpStatus.FORBIDDEN),

    INVALID_VERIFICATION_CODE("INVALID_VERIFICATION_CODE", "Invalid verification code", HttpStatus.BAD_REQUEST),

    USER_PROFILE_ALREADY_EXISTS("USER_PROFILE_ALREADY_EXISTS", "User profile already exists", HttpStatus.CONFLICT),
    USER_PROFILE_NOT_FOUND("USER_PROFILE_NOT_FOUND", "User profile not found", HttpStatus.NOT_FOUND),

    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    MEDIA_NOT_USER("MEDIA_NOT_USER", "Media file does not belong to the user", HttpStatus.FORBIDDEN),
    INVALID_MEDIA_URL("INVALID_MEDIA_URL", "Invalid media URL", HttpStatus.BAD_REQUEST),
    MEDIA_NOT_FOUND("MEDIA_NOT_FOUND", "Media file not found", HttpStatus.NOT_FOUND),

    AI_SERVER_UNAVAILABLE("AI_SERVER_UNAVAILABLE", "AI server is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    AI_DETECTION_FAILED("AI_DETECTION_FAILED", "AI detection failed", HttpStatus.INTERNAL_SERVER_ERROR),

    CREDIT_INSUFFICIENT("CREDIT_INSUFFICIENT", "Insufficient credits", HttpStatus.BAD_REQUEST),

    SCAN_JOB_NOT_FOUND("SCAN_JOB_NOT_FOUND", "Scan job not found", HttpStatus.NOT_FOUND),
    REPORT_EXPORT_FAILED("REPORT_EXPORT_FAILED", "Failed to generate PDF report", HttpStatus.INTERNAL_SERVER_ERROR)
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}

