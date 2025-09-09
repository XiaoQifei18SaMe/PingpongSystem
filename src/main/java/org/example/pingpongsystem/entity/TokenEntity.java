package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class TokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String token;

    private boolean isSuperAdmin;

    private boolean isAdmin;

    private boolean isCoach;

    private boolean isStudent;

    private Long userId;

    @Version
    private Integer version;
}
