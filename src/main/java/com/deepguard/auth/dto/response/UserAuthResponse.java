package com.deepguard.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResponse {

    private String id;
    private String email;
    private String username;
    private String role;
    private String status;
    private boolean verified;
}
