package com.deepguard.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResponse {

    private UUID id;
    private String email;
    private String username;
    private String role;
    private String status;
    private boolean verified;
}
