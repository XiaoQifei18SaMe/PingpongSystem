package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.SuperAdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuperAdminRepository extends JpaRepository<SuperAdminEntity, Long> {

    SuperAdminEntity findByUsername(String name);
}
