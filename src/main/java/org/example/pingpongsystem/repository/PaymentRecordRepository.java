package org.example.pingpongsystem.repository;

import org.example.pingpongsystem.entity.PaymentRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecordEntity, Long> {
}
