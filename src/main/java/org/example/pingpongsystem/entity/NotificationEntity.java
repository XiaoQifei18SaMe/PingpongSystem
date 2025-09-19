package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // 接收通知的用户ID（学员或教练）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType; // 用户类型：STUDENT/COACH

    @Column(nullable = false)
    private Long appointmentId; // 关联的课程预约ID

    @Column(nullable = false)
    private String content; // 通知内容（如："您有一节课程需要评价"）

    private boolean isRead = false; //  是否已读

    private LocalDateTime createTime; // 通知创建时间

    // 用户类型枚举
    public enum UserType {
        STUDENT, COACH
    }
}