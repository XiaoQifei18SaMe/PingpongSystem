package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoachEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoachRepository extends JpaRepository<CoachEntity, Long> {

    CoachEntity findByUsername(String name);

    List<CoachEntity> findAllByCertified(boolean certified);
}
