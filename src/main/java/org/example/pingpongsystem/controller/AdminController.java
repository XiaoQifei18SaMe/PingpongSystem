package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.service.AdminService;
import org.example.pingpongsystem.service.CoachService;
import org.example.pingpongsystem.service.SuperAdminService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequestMapping("/admin")
@RestController
public class AdminController {
    private final AdminService adminService;
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody SuperAdminController.LoginRequest request) {
        return adminService.login(request.getUsername(), request.getPassword());
    }

    @GetMapping("/get_coach_register")
    public Result<List<CoachEntity>> getCoachRegister(@RequestParam Long schoolId) {
        return adminService.getUncertifiedCoach(schoolId);
    }

    @PostMapping("/certify_coach")
    public Result<CoachEntity> certifyCoach(@RequestParam Long coachId, @RequestParam boolean isAccepted) {
        return adminService.certifyCoach(coachId, isAccepted);
    }
}
