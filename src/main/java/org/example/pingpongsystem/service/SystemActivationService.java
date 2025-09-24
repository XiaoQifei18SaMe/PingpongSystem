package org.example.pingpongsystem.service;

import org.example.pingpongsystem.entity.SystemActivation;
import org.example.pingpongsystem.repository.SystemActivationRepository;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class SystemActivationService {
    private final SystemActivationRepository activationRepository;
    private final PaymentService paymentService;

    public SystemActivationService(SystemActivationRepository activationRepository, PaymentService paymentService) {
        this.activationRepository = activationRepository;
        this.paymentService = paymentService;
    }

    // 检查系统是否已激活（所有用户访问前需验证）
    public boolean isSystemActivated() {
        return activationRepository.findTopByIsActiveTrue()
                .map(activation -> LocalDateTime.now().isBefore(activation.getValidTo()))
                .orElse(false);
    }

    // 超级管理员发起服务费支付（模拟，金额500元/年）
    @Transactional
    public Result<String> payServiceFee(Long superAdminId) {
        // 模拟支付流程（参考学生支付逻辑）
        // 生成支付记录（金额500元，类型为系统服务费）
        // 此处简化为直接返回支付成功
        return Result.success("支付成功，请激活系统");
    }

    // 激活系统（生成秘钥+绑定设备）
    @Transactional
    public Result<SystemActivation> activateSystem(Long superAdminId, String deviceId) {
        // 1. 生成唯一秘钥
        String secretKey = "SYS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 2. 设置有效期（1年）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validTo = now.plusYears(1);

        // 3. 关闭之前的激活记录（确保唯一激活）
        activationRepository.findTopByIsActiveTrue().ifPresent(activation -> {
            activation.setActive(false);
            activationRepository.save(activation);
        });

        // 4. 创建新激活记录
        SystemActivation activation = new SystemActivation();
        activation.setSecretKey(secretKey);
        activation.setDeviceId(deviceId);
        activation.setValidFrom(now);
        activation.setValidTo(validTo);
        activation.setActive(true);
        activation.setSuperAdminId(superAdminId);

        SystemActivation saved = activationRepository.save(activation);
        return Result.success(saved);
    }

    // 验证设备是否匹配（防止秘钥滥用）
    public Result<Boolean> verifyDevice(String secretKey, String deviceId) {
        return activationRepository.findBySecretKey(secretKey)
                .map(activation -> {
                    boolean isMatch = activation.getDeviceId().equals(deviceId) && activation.isActive();
                    return Result.success(isMatch);
                })
                .orElse(Result.error(StatusCode.FAIL, "无效的秘钥"));
    }

    // 新增：获取当前激活的记录
    public Optional<SystemActivation> getCurrentActiveActivation() {
        return activationRepository.findTopByIsActiveTrue();
    }
}
