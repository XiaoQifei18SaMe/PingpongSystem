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

    public String save(StudentEntity student) {
        try {
            studentRepository.save(student);
            return "保存成功";
        }
        catch (DataAccessException e) {
            System.err.println("保存学生信息失败：" + e.getMessage());
            return "用户名、密码、校区、姓名和电话是必须填写的信息，其他信息可空白";
        }
    }
}
