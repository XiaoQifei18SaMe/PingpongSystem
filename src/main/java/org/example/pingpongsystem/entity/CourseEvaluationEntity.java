package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class CourseEvaluationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long appointmentId;  // 关联的课程预约ID

    @Column(nullable = false)
    private Long evaluatorId;    // 评价人ID（学员或教练）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluatorType evaluatorType;  // 评价人类型

    private String content;  // 评价内容

    private LocalDateTime createTime;  // 评价创建时间

    // 评价人类型枚举
    public enum EvaluatorType {
        STUDENT,  // 学员
        COACH     // 教练
    }
}
