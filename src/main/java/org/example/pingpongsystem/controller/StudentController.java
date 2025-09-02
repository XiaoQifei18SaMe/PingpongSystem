package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.StudentEntity;
import org.example.pingpongsystem.service.StudentService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/student")
@RestController
public class StudentController {
    private final StudentService studentService;
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/create_user")
    public String createUser(@RequestBody StudentEntity studentEntity) {
        studentEntity.setId(null);
        studentEntity.setVersion(null);
        boolean result = studentService.save(studentEntity);
        if (result) {
            return "保存成功";
        }
        else {
            return "用户名、密码、校区、姓名和电话是必须填写的信息，其他信息可空白";
        }
    }

    @PostMapping("/login")
    public Result<StudentEntity> login(@RequestParam String username, @RequestParam String password) {
        return studentService.login(username, password);
    }

    @PostMapping("/update_info")
    public Result<StudentEntity> updateInfo(@RequestBody StudentEntity studentEntity) {
        return studentService.revise(studentEntity);
    }
}
