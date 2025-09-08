package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.service.AdminService;
import org.example.pingpongsystem.service.CoachService;
import org.example.pingpongsystem.service.SuperAdminService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/coach")
@RestController
public class SuperAdminController {
    private final SuperAdminService superAdminService;
    private final AdminService adminService;

    public SuperAdminController(SuperAdminService superAdminService, AdminService adminService) {
        this.superAdminService = superAdminService;
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Result<SuperAdminEntity> login(@RequestParam String username, @RequestParam String password) {
        return superAdminService.login(username, password);
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
}
