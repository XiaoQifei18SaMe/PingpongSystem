package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CancelRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CancelRecordRepository extends JpaRepository<CancelRecordEntity, Long> {
    // 查询用户当月取消次数
    long countByUserIdAndUserTypeAndCreateTimeBetween(
            Long userId, String userType, LocalDateTime monthStart, LocalDateTime monthEnd);

    // 查询预约相关的取消记录
    CancelRecordEntity findByAppointmentId(Long appointmentId);

    // 根据用户ID、用户类型、状态查询取消申请记录
    List<CancelRecordEntity> findByUserIdAndUserTypeAndStatus(
            Long userId,
            String userType,
            CancelRecordEntity.CancelStatus status
    );
}