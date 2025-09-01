package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.testEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRepository extends JpaRepository<testEntity, Long> {
}