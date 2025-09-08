package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.TableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TableRepository extends JpaRepository<TableEntity, Long> {
    List<TableEntity> findAllBySchoolId(Long schoolId);
}
