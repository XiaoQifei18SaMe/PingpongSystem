package org.example.pingpongsystem.controller;

import lombok.Data;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.service.*;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.example.pingpongsystem.utility.interfaces.InfoAns;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RequestMapping("/super_admin")
@RestController
public class SuperAdminController {
    private final SuperAdminService superAdminService;
    private final AdminService adminService;
    private final PaymentService paymentService;
    private final SystemActivationService activationService;

    public SuperAdminController(SuperAdminService superAdminService,
                                AdminService adminService,
                                PaymentService paymentService,
                                SystemActivationService systemActivationService) {
        this.superAdminService = superAdminService;
        this.adminService = adminService;
        this.paymentService = paymentService;
        this.activationService = systemActivationService;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest request) {
        return superAdminService.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/create_admin")
    public Result<AdminEntity> createUser(@RequestBody AdminEntity adminEntity) {
        return superAdminService.createAdmin(adminEntity);
    }

    // 新增：编辑管理员
    @PostMapping("/edit_admin")
    public Result<AdminEntity> editAdmin(@RequestBody AdminEntity adminEntity) {
        return superAdminService.editAdmin(adminEntity);
    }

    // 新增：删除管理员
    @DeleteMapping("/delete_admin/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        return superAdminService.deleteAdmin(id);
    }

    // 新增：获取所有管理员列表（便于前端展示编辑/删除目标）
    @GetMapping("/admins")
    public Result<List<AdminEntity>> getAllAdmins() {
        return superAdminService.getAllAdmins();
    }

    @GetMapping("/schools")
    public Result<List<SchoolEntity>> getAllSchools() {
        return superAdminService.getAllSchools();
    }

    @PostMapping("/create_school")
    public Result<SchoolEntity> createSchool(@RequestBody SchoolRequest request) {
        SchoolEntity schoolEntity = new SchoolEntity();
        schoolEntity.setSchoolname(request.getSchoolname()); // 新增：设置校区名称
        schoolEntity.setName(request.getName());
        schoolEntity.setAddress(request.getLocation());
        schoolEntity.setTable_num(request.getTableCount());
        schoolEntity.setAdminId(request.getAdminId());
        // 补充设置其他字段（phone、email等，前端已传递）
        schoolEntity.setPhone(request.getPhone()); // 如果SchoolRequest中也添加了phone
        schoolEntity.setEmail(request.getEmail()); // 如果SchoolRequest中也添加了email
        return superAdminService.createSchool(schoolEntity);
    }

    @PostMapping("/manage_school_info")
    public Result<SchoolEntity> manageSchoolInfo(@RequestBody SchoolEntity schoolEntity) {
        return superAdminService.reviseSchool(schoolEntity);
    }

    @DeleteMapping("/delete_school/{id}")
    public Result<Void> deleteSchool(@PathVariable Long id) {
        return superAdminService.deleteSchool(id);
    }

    // 超级管理员教练审核
    //获取所有待审核的教练
    @GetMapping("/get_all_uncertified_coaches")
    public Result<List<CoachEntity>> getAllUncertifiedCoaches() {
        return superAdminService.getAllUncertifiedCoaches();
    }

    //审核教练
    @PostMapping("/super_certify_coach")
    public Result<CoachEntity> superCertifyCoach(@RequestBody AdminController.CertifyRequest request) {
        return superAdminService.superCertifyCoach(request.getCoachId(), request.getIsAccepted(),request.getLevel());
    }

    //获取教练详情
    @GetMapping("/get_super_coach_detail")
    public Result<CoachEntity> getSuperCoachDetail(@RequestParam Long coachId) {
        return superAdminService.getSuperCoachDetail(coachId);
    }
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private String role; // 新增角色字段
    }

    @Data
    public static class SchoolRequest {
        private String name;
        private String location;
        private int tableCount;
        private Long adminId;
        private String schoolname; // 校区名称
        private String phone; // 联系电话
        private String email; // 联系邮箱
    }

    @PostMapping("/upload_avatar")
    public Result<String> uploadAvatar(
            @RequestParam Long superAdminId,
            @RequestParam MultipartFile file
    ) {
        return superAdminService.uploadAvatar(superAdminId, file);
    }

    @PostMapping("/update_info")
    public Result<SuperAdminEntity> updateSuperAdminInfo(@RequestBody InfoAns info) {
        return superAdminService.updateInfo(info);
    }

    // 新增：分页查询所有校区学生（支持筛选）
    @GetMapping("/all-students")
    public Result<Page<StudentEntity>> getAllStudents(
            @RequestParam String token,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return superAdminService.getAllStudentsWithPage(token, schoolId, name, pageNum, pageSize);
    }

    // 新增：分页查询所有已认证教练（支持筛选）
    @GetMapping("/all-certified-coaches")
    public Result<Page<CoachEntity>> getAllCertifiedCoaches(
            @RequestParam String token,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer level,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return superAdminService.getAllCertifiedCoachesWithPage(token, schoolId, name, level, pageNum, pageSize);
    }

    // 新增：超级管理员更新学生信息
    @PostMapping("/update-student")
    public Result<StudentEntity> updateStudent(
            @RequestParam String token,
            @RequestBody StudentEntity student) {
        return superAdminService.updateStudent(token, student);
    }

    // 新增：超级管理员更新已认证教练信息
    @PostMapping("/update-certified-coach")
    public Result<CoachEntity> updateCertifiedCoach(
            @RequestParam String token,
            @RequestBody CoachEntity coach) {
        return superAdminService.updateCertifiedCoach(token, coach);
    }

    @PostMapping("/recharge")
    public Result<PaymentRecordEntity> superAdminOfflineRecharge(
            @RequestParam Long studentId,
            @RequestParam Double amount) {  // 添加token参数用于权限验证
        // 可以在这里添加超级管理员权限验证逻辑
        return paymentService.adminOfflineRecharge(studentId, amount);
    }

    // 发起服务费支付（500元/年）
    @GetMapping("/pay_service_fee")
    public Result<String> payServiceFee(@RequestParam Long superAdminId) {
        return activationService.payServiceFee(superAdminId);
    }

    // 激活系统（需支付后调用）
    @PostMapping("/activate_system")
    public Result<SystemActivation> activateSystem(
            @RequestParam Long superAdminId,
            @RequestParam String deviceId) { // 前端传递的设备唯一标识
        return activationService.activateSystem(superAdminId, deviceId);
    }

    @GetMapping("/verify_activation")
    public Result<Boolean> verifyActivation(@RequestParam String deviceId) {
        // 调用Service中新增的方法获取当前激活记录
        Optional<SystemActivation> activeActivation = activationService.getCurrentActiveActivation();
        if (activeActivation.isEmpty()) {
            return Result.error(StatusCode.FAIL, "系统未激活");
        }
        // 验证设备与当前激活记录的匹配性
        return activationService.verifyDevice(activeActivation.get().getSecretKey(), deviceId);
    }

}
