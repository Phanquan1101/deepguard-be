package com.deepguard.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for updating a user's account status.
 * Valid values: ACTIVE | SUSPENDED | DELETED
 */
@Data
public class UpdateUserStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;
}
