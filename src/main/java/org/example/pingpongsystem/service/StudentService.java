package org.example.pingpongsystem.service;

import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.repository.StudentRepository;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Result<StudentEntity> login(String username, String password) {
        StudentEntity temp = studentRepository.findByUsername(username);
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "用户名不存在");
        }
        else if (!temp.getPassword().equals(password)) {
            return Result.error(StatusCode.PASSWORD_ERROR, "密码错误");
        }
        return Result.success(temp);
    }

    @Transactional
    public Result<StudentEntity> revise(StudentEntity student) {
        StudentEntity temp = studentRepository.findByUsername(student.getUsername());
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "用户名不存在");
        }
        else {
            if (!student.getPassword().equals(temp.getPassword())) {
                if (!student.getPassword().isEmpty())
                    temp.setPassword(student.getPassword());
            }
            if (!student.getName().equals(temp.getName())) {
                if (!student.getName().isEmpty())
                    temp.setName(student.getName());
            }
            if (student.isMale() != temp.isMale()) {
                temp.setMale(student.isMale());
            }
            if (student.getAge() != temp.getAge()) {
                if (student.getAge() > 0 && student.getAge() < 200)
                    temp.setAge(student.getAge());
            }
            if (!student.getPhone().equals(temp.getPhone())) {
                if (!student.getPhone().isEmpty())
                    temp.setPhone(student.getPhone());
            }
            if (!student.getEmail().equals(temp.getEmail())) {
                if (!student.getEmail().isEmpty())
                    temp.setEmail(student.getEmail());
            }
            return Result.success(temp);
        }
    }
}
