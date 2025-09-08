package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class CoachTeachStudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private Long coachId;

    @NotBlank
    @Column(nullable = false)
    private Long studentId;

    private boolean isConfirmed;

    @Version
    private Integer version;
}
