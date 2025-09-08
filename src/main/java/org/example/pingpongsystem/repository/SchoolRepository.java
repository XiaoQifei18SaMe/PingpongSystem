package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<SchoolEntity, Long> {

    SchoolEntity findBySchoolname(String name);
}
