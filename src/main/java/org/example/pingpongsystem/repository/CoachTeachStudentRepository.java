package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoachTeachStudentEntity;
import org.example.pingpongsystem.entity.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoachTeachStudentRepository extends JpaRepository<CoachTeachStudentEntity, Long> {
    public int countByCoachId(Long coachId);
    public int countByStudentId(Long studentId);

    public List<CoachTeachStudentEntity> findByCoachId(Long coachId);
}
