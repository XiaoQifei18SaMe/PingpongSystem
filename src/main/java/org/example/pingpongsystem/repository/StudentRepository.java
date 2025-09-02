package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    StudentEntity findByUsername(String name);
}
