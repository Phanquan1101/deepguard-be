package com.deepguard.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import com.deepguard.auth.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_credits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredit {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "remaining_credits", nullable = false)
    private Integer remainingCredits;

    @Column(name = "used_credits", nullable = false)
    private Integer usedCredits;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();

        if (remainingCredits == null) {
            remainingCredits = 0;
        }

        if (usedCredits == null) {
            usedCredits = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}



