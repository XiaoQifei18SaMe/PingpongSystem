package org.example.pingpongsystem.service;

import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.repository.StudentRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
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
        } catch (OptimisticLockingFailureException e) {
            System.err.println("数据已被其他用户修改，请刷新后重试");
            return false;
        } catch (ConstraintViolationException e) {
            System.err.println("必需字段空缺");
            return false;
        } catch (DataAccessException e) {
            System.err.println("保存学生信息失败：" + e.getMessage());
            return false;
        }
    }
}
