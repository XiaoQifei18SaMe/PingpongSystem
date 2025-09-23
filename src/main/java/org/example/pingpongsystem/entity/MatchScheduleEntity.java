package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class MatchScheduleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long monthlyMatchId; // 关联月赛ID

    @Column(nullable = false)
    private Long groupId; // 关联小组ID

    @Column(nullable = false)
    private Integer roundNumber; // 轮次

    private Long player1Id; // 选手1 ID

    private Long player2Id; // 选手2 ID（可能为null表示轮空）

    @Column(nullable = false)
    private Long tableId; // 球台ID

    private LocalDateTime startTime; // 比赛开始时间

    @Enumerated(EnumType.STRING)
    private MatchResult result; // 比赛结果

    public enum MatchResult {
        NOT_STARTED, PLAYER1_WIN, PLAYER2_WIN, DRAW
    }
}
