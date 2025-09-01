package org.example.pingpongsystem.service;

import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.repository.StudentRepository;
import org.example.pingpongsystem.repository.TestRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class StudentService {
    private final StudentRepository studentRepository;
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;  // 由Spring容器注入实例
    }

    public boolean save(StudentEntity student) {
        try {
            studentRepository.save(student);
            return true;
        }
        catch (DataAccessException e) {
            System.err.println("保存学生信息失败：" + e.getMessage());
            return false;
        }
    }
}
