package org.example.pingpongsystem.service;

import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.repository.*;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.example.pingpongsystem.utility.interfaces.InfoAns;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {
    private final TokenRepository tokenRepository;
    private final SuperAdminRepository superAdminRepository;
    private final AdminRepository adminRepository;
    private final CoachRepository coachRepository;
    private final StudentRepository studentRepository;

    public TokenService(TokenRepository tokenRepository, SuperAdminRepository superAdminRepository, AdminRepository adminRepository, CoachRepository coachRepository, StudentRepository studentRepository) {
        this.tokenRepository = tokenRepository;
        this.superAdminRepository = superAdminRepository;
        this.adminRepository = adminRepository;
        this.coachRepository = coachRepository;
        this.studentRepository = studentRepository;
    }

    public Result<InfoAns> getInfo(String token) {
        TokenEntity tokenEntity = tokenRepository.findByToken(token);
        if (tokenEntity != null) {
            if (tokenEntity.isSuperAdmin()) {
                Optional<SuperAdminEntity> admin = superAdminRepository.findById(tokenEntity.getId());
                if (admin.isPresent()) {
                    InfoAns infoAns = new InfoAns();
                    infoAns.setUsername(admin.get().getUsername());
                    infoAns.setPassword(admin.get().getPassword());
                    infoAns.setPhone(admin.get().getPhone());
                    infoAns.setEmail(admin.get().getEmail());
                    return Result.success(infoAns);
                }
                else return Result.error(StatusCode.FAIL, "超级管理员未找到");
            }
            if (tokenEntity.isAdmin()) {
                Optional<AdminEntity> admin = adminRepository.findById(tokenEntity.getId());
                if (admin.isPresent()) {
                    InfoAns infoAns = new InfoAns();
                    infoAns.setUsername(admin.get().getUsername());
                    infoAns.setPassword(admin.get().getPassword());
                    infoAns.setPhone(admin.get().getPhone());
                    infoAns.setEmail(admin.get().getEmail());
                    return Result.success(infoAns);
                }
                else return Result.error(StatusCode.FAIL, "管理员未找到");
            }
            if (tokenEntity.isCoach()) {
                Optional<CoachEntity> admin = coachRepository.findById(tokenEntity.getId());
                if (admin.isPresent()) {
                    InfoAns infoAns = new InfoAns();
                    infoAns.setUsername(admin.get().getUsername());
                    infoAns.setPassword(admin.get().getPassword());
                    infoAns.setAge(admin.get().getAge());
                    infoAns.setMale(admin.get().isMale());
                    infoAns.setPhone(admin.get().getPhone());
                    infoAns.setEmail(admin.get().getEmail());
                    infoAns.setSchoolId(admin.get().getSchoolId());
                    return Result.success(infoAns);
                }
                else return Result.error(StatusCode.FAIL, "教练未找到");
            }
            if (tokenEntity.isStudent()) {
                Optional<StudentEntity> admin = studentRepository.findById(tokenEntity.getId());
                if (admin.isPresent()) {
                    InfoAns infoAns = new InfoAns();
                    infoAns.setUsername(admin.get().getUsername());
                    infoAns.setPassword(admin.get().getPassword());
                    infoAns.setAge(admin.get().getAge());
                    infoAns.setMale(admin.get().isMale());
                    infoAns.setPhone(admin.get().getPhone());
                    infoAns.setEmail(admin.get().getEmail());
                    infoAns.setSchoolId(admin.get().getSchoolId());
                    return Result.success(infoAns);
                }
                else return Result.error(StatusCode.FAIL, "学员未找到");
            }
            return Result.error(StatusCode.FAIL, "检查token数据表");
        }
        else
            return Result.error(StatusCode.FAIL, "token不存在");
    }

    public Result<String> createToken(boolean isSuperAdmin, boolean isAdmin, boolean isCoach, boolean isStudent, Long userId) {
        try {
            TokenEntity tokenEntity = new TokenEntity();
            tokenEntity.setToken(UUID.randomUUID().toString());
            tokenEntity.setSuperAdmin(isSuperAdmin);
            tokenEntity.setAdmin(isAdmin);
            tokenEntity.setCoach(isCoach);
            tokenEntity.setStudent(isStudent);
            tokenEntity.setUserId(userId);
            tokenRepository.save(tokenEntity);
            return Result.success(tokenEntity.getToken());
        } catch (OptimisticLockingFailureException e) {
            System.err.println("数据已被其他用户修改，请刷新后重试");
            return Result.error(StatusCode.FAIL, "数据已被其他用户修改，请刷新后重试");
        } catch (ConstraintViolationException e) {
            System.err.println("必需字段空缺");
            return Result.error(StatusCode.FAIL, "必需字段空缺");
        } catch (DataAccessException e) {
            System.err.println("保存token信息失败：" + e.getMessage());
            return Result.error(StatusCode.FAIL, "保存token信息失败");
        }
    }
}
