package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.service.StudentService;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user")
@RestController
public class StudentController {
    private final StudentService studentService;
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/create_user")
    public String createUser(@RequestBody StudentEntity studentEntity) {
        boolean result = studentService.save(studentEntity);
        if (result) {
            return "保存成功";
        }
        else {
            return "用户名、密码、校区、姓名和电话是必须填写的信息，其他信息可空白";
        }
    }
}
