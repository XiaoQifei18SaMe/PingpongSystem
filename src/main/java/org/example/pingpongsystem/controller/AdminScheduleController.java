package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.ScheduleEntity;
import org.example.pingpongsystem.entity.SchoolEntity;
import org.example.pingpongsystem.service.AdminService;
import org.example.pingpongsystem.service.ScheduleService;
import org.example.pingpongsystem.service.TokenService;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.interfaces.InfoAns;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/admin/schedule")
@RestController
public class AdminScheduleController {
    private final ScheduleService scheduleService;
    private final TokenService tokenService;

    public AdminScheduleController(ScheduleService scheduleService, TokenService tokenService) {
        this.scheduleService = scheduleService;
        this.tokenService = tokenService;
    }

    // 获取默认课表模板
    @GetMapping("/default")
    public Result<List<ScheduleEntity>> getDefaultSchedule() {
        return scheduleService.getDefaultSchedule();
    }

    // 获取管辖校区
    @GetMapping("/managed-schools")
    public Result<List<SchoolEntity>> getManagedSchools(@RequestParam String token) {
        Result<InfoAns> infoResult = tokenService.getInfo(token);
        if (!infoResult.isSuccess()) {
            return Result.error(infoResult.getCode(), infoResult.getMessage());
        }
        return scheduleService.getManagedSchools(Long.valueOf(infoResult.getData().getUserId()));
    }

    // 检查校区是否已有课表
    @GetMapping("/check/{schoolId}")
    public Result<Boolean> checkSchoolSchedule(@PathVariable Long schoolId) {
        return scheduleService.hasSchedule(schoolId);
    }

    // 保存课表到指定校区
    @PostMapping("/save/{schoolId}")
    public Result<Void> saveSchedule(
            @PathVariable Long schoolId,
            @RequestBody List<ScheduleEntity> schedules
    ) {
        return scheduleService.saveSchedule(schoolId, schedules);
    }

    // 获取校区课表
    @GetMapping("/school/{schoolId}")
    public Result<List<ScheduleEntity>> getSchoolSchedule(@PathVariable Long schoolId) {
        return scheduleService.getSchoolSchedule(schoolId);
    }
}