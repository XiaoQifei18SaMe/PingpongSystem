package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class StudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private boolean isMale;

    private int age;

    @Column(nullable = false)
    private int schoolId;

    @Column(nullable = false)
    private String phone;

    private String email;
}
