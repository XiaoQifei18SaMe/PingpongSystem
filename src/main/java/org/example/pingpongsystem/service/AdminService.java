package org.example.pingpongsystem.service;

import jakarta.validation.ConstraintViolationException;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.repository.*;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {
    private final SchoolRepository schoolRepository;
    private final TableRepository tableRepository;
    private final AdminRepository adminRepository;
    private final CoachRepository coachRepository;
    private final TokenService tokenService;

    public AdminService(SchoolRepository schoolRepository, TableRepository tableRepository, AdminRepository adminRepository, CoachRepository coachRepository, TokenService tokenService) {
        this.schoolRepository = schoolRepository;
        this.tableRepository = tableRepository;
        this.adminRepository = adminRepository;
        this.coachRepository = coachRepository;
        this.tokenService = tokenService;
    }

    public Result<String> login(String username, String password) {
        AdminEntity temp = adminRepository.findByUsername(username);
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "用户名不存在");
        }
        else if (!temp.getPassword().equals(password)) {
            return Result.error(StatusCode.PASSWORD_ERROR, "密码错误");
        }
        return tokenService.createToken(false, true, false, false, temp.getId());
    }

    public Result<List<CoachEntity>> getUncertifiedCoach(Long schoolId) {
        List<CoachEntity> tmp = coachRepository.findAllByisCertified(false);
        List<CoachEntity> list = new ArrayList<>();
        for (CoachEntity c : tmp) {
            if (c.getSchoolId() == schoolId)
                list.add(c);
        }
        return Result.success(list);
    }

    @Transactional
    public Result<CoachEntity> certifyCoach(Long coachId, boolean isAccepted) {
        Optional<CoachEntity> tmp = coachRepository.findById(coachId);
        if (tmp.isPresent()) {
            CoachEntity coach = tmp.get();
            if (isAccepted) {
                coach.setCertified(true);
                return Result.success(coachRepository.save(coach));
            }
            else {
                coachRepository.delete(coach);
                return Result.success();
            }
        }
        else return Result.error(StatusCode.FAIL, "该教练不存在");
    }

    @Transactional
    public Result<SchoolEntity> reviseSchool(SchoolEntity school) {
        SchoolEntity temp = schoolRepository.findBySchoolname(school.getSchoolname());
        if (temp == null) {
            return Result.error(StatusCode.USERNAME_NOT_FOUND, "学校不存在");
        }
        else {
            if (!school.getAddress().equals(temp.getAddress())) {
                if (!school.getAddress().isEmpty())
                    temp.setAddress(school.getAddress());
            }
            if (!school.getName().equals(temp.getName())) {
                if (!school.getName().isEmpty())
                    temp.setName(school.getName());
            }
            if (!school.getPhone().equals(temp.getPhone())) {
                if (!school.getPhone().isEmpty())
                    temp.setPhone(school.getPhone());
            }
            if (!school.getEmail().equals(temp.getEmail())) {
                if (!school.getEmail().isEmpty())
                    temp.setEmail(school.getEmail());
            }
            if (school.getAdminId() != temp.getAdminId()) {
                temp.setAdminId(school.getAdminId());
            }
            if (school.getTable_num() != temp.getTable_num()) {
                List<TableEntity> li = tableRepository.findAllBySchoolId(school.getId());
                tableRepository.deleteAll(li);

                for (int i=0;i<school.getTable_num();i++) {
                    TableEntity tableEntity = new TableEntity();
                    tableEntity.setOccupied(false);
                    tableEntity.setSchoolId(school.getId());
                    tableRepository.save(tableEntity);
                }

                temp.setTable_num(school.getTable_num());
            }
            return Result.success(temp);
        }
    }
}
