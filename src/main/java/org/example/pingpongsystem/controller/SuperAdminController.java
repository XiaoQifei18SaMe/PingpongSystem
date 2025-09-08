package org.example.pingpongsystem.controller;

import lombok.Data;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.service.AdminService;
import org.example.pingpongsystem.service.CoachService;
import org.example.pingpongsystem.service.SuperAdminService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/super_admin")
@RestController
public class SuperAdminController {
    private final SuperAdminService superAdminService;
    private final AdminService adminService;

    public SuperAdminController(SuperAdminService superAdminService, AdminService adminService) {
        this.superAdminService = superAdminService;
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Result<SuperAdminEntity> login(@RequestBody LoginRequest request) {
        return superAdminService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/create_admin")
    public Result<AdminEntity> createUser(@RequestBody AdminEntity adminEntity) {
        return superAdminService.createAdmin(adminEntity);
    }

    @PostMapping("/create_school")
    public Result<SchoolEntity> createSchool(@RequestBody SchoolEntity schoolEntity) {
        return superAdminService.createSchool(schoolEntity);
    }

    @PostMapping("/manage_school_info")
    public Result<SchoolEntity> manageSchoolInfo(@RequestBody SchoolEntity schoolEntity) {
        return superAdminService.reviseSchool(schoolEntity);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private String role; // 新增角色字段
    }
}
