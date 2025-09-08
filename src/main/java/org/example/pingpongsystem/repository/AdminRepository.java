package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<AdminEntity, Long> {

    AdminEntity findByUsername(String name);
}
