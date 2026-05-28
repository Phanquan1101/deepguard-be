package com.deepguard.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import com.deepguard.auth.entity.User;
import com.deepguard.billing.enums.ActionType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditRule {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(name = "credit_cost", nullable = false)
    private Integer creditCost;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}



