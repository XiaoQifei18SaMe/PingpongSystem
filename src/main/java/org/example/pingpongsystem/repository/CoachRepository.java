package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.CoachEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachRepository extends JpaRepository<CoachEntity, Long> {

    CoachEntity findByUsername(String name);
}
