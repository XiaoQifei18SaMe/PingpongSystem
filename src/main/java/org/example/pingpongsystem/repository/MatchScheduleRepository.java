package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.MatchScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchScheduleRepository extends JpaRepository<MatchScheduleEntity, Long> {
    List<MatchScheduleEntity> findByMonthlyMatchId(Long matchId);
    List<MatchScheduleEntity> findByMonthlyMatchIdAndGroupId(Long matchId, Long groupId);
    List<MatchScheduleEntity> findByMonthlyMatchIdAndPlayer1IdOrPlayer2Id(Long matchId, Long player1Id, Long player2Id);
}