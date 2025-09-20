package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    StudentEntity findByUsername(String name);
    List<StudentEntity> findBySchoolId(Long schoolId); // 新增
}
