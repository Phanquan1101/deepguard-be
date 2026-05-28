package com.deepguard.billing.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreditResponse {
    private String userId;
    private Integer remainingCredits;
    private Integer usedCredits;
}
