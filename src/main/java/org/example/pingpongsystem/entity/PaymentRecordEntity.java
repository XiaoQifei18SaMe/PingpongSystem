package org.example.pingpongsystem.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payment_record")
public class PaymentRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId; // 学员ID

    @Column(nullable = false)
    private Double amount; // 支付金额

    @Column(nullable = false)
    private String paymentMethod; // 支付方式：WECHAT/ALIPAY/OFFLINE

    private String qrCodeUrl; // 二维码图片地址（线上支付用）

    private String status; // 状态：PENDING/SUCCESS/FAILED/REFUNDED

    private LocalDateTime createTime; // 创建时间

    private LocalDateTime payTime; // 支付时间

    private LocalDateTime refundTime; // 退款时间（新增字段）
}