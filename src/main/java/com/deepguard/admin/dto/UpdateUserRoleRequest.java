package com.deepguard.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for changing a user's role.
 * Role name must match an existing role in the roles table (e.g. "ROLE_USER", "ROLE_ADMIN").
 */
@Data
public class UpdateUserRoleRequest {

    @NotBlank(message = "Role name is required")
    private String roleName;
}
