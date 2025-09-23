package org.example.pingpongsystem.controller;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.MatchRegistrationEntity;
import org.example.pingpongsystem.entity.MatchScheduleEntity;
import org.example.pingpongsystem.entity.MonthlyMatchEntity;
import org.example.pingpongsystem.service.MonthlyMatchService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequestMapping("/monthly-match")
@RestController
@RequiredArgsConstructor
public class MonthlyMatchController {

    private final MonthlyMatchService matchService;

    // 获取当前可报名的月赛
    @GetMapping("/available")
    public Result<List<MonthlyMatchEntity>> getAvailableMatches() {
        return matchService.getAvailableMatches();
    }

    // 学员报名月赛
    @PostMapping("/register")
    public Result<MatchRegistrationEntity> register(
            @RequestParam Long matchId,
            @RequestParam Long studentId,
            @RequestParam String groupType) { // GROUP_A, GROUP_B, GROUP_C
        return matchService.registerForMatch(matchId, studentId, MatchRegistrationEntity.GroupType.valueOf(groupType));
    }

    // 获取学员的报名记录
    @GetMapping("/student-registrations")
    public Result<List<MatchRegistrationEntity>> getStudentRegistrations(@RequestParam Long studentId) {
        return matchService.getStudentRegistrations(studentId);
    }

    // 获取月赛各组别报名人数
    @GetMapping("/group-counts")
    public Result<Map<MatchRegistrationEntity.GroupType, Long>> getGroupCounts(@RequestParam Long matchId) {
        return matchService.getGroupRegistrationCounts(matchId);
    }

    // 获取学员的比赛安排
    @GetMapping("/student-schedule")
    public Result<List<MatchScheduleEntity>> getStudentSchedule(
            @RequestParam Long matchId,
            @RequestParam Long studentId) {
        return matchService.getStudentMatchSchedule(matchId, studentId);
    }

    // 管理员修改比赛时间
    @PostMapping("/admin/update-time")
    public Result<MonthlyMatchEntity> updateMatchTime(
            @RequestParam Long matchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime) {
        return matchService.updateMatchTime(matchId, startTime);
    }

    /**
     * 获取单个月赛详情
     */
    @GetMapping("/get-match")
    public Result<MonthlyMatchEntity> getMatchById(@RequestParam Long matchId) {
        return matchService.getMatchById(matchId);
    }
}
