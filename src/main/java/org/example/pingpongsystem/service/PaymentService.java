package org.example.pingpongsystem.service;

import org.example.pingpongsystem.entity.PaymentRecordEntity;
import org.example.pingpongsystem.entity.StudentAccountEntity;
import org.example.pingpongsystem.repository.PaymentRecordRepository;
import org.example.pingpongsystem.repository.StudentAccountRepository;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    private final StudentAccountRepository accountRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentService(StudentAccountRepository accountRepository, PaymentRecordRepository paymentRecordRepository) {
        this.accountRepository = accountRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    // 获取学员账户余额
    public Result<Double> getBalance(Long studentId) {
        StudentAccountEntity account = accountRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    // 首次查询创建账户
                    StudentAccountEntity newAccount = new StudentAccountEntity();
                    newAccount.setStudentId(studentId);
                    return accountRepository.save(newAccount);
                });
        return Result.success(account.getBalance());
    }

    // 创建支付订单（生成二维码）
    public Result<PaymentRecordEntity> createPayment(Long studentId, Double amount, String method) {
        if (amount <= 0) {
            return Result.error(StatusCode.FAIL, "金额必须大于0");
        }
        if (!"WECHAT".equals(method) && !"ALIPAY".equals(method)) {
            return Result.error(StatusCode.FAIL, "不支持的支付方式");
        }

        PaymentRecordEntity record = new PaymentRecordEntity();
        record.setStudentId(studentId);
        record.setAmount(amount);
        record.setPaymentMethod(method);
        record.setStatus("PENDING");
        record.setCreateTime(LocalDateTime.now());
        // 模拟二维码地址（实际项目需对接支付平台生成真实二维码）
        record.setQrCodeUrl("/qrcode/" + UUID.randomUUID() + ".png");

        paymentRecordRepository.save(record);
        return Result.success(record);
    }

    // 模拟支付结果回调（扫码后确认支付）
    @Transactional
    public Result<String> confirmPayment(Long recordId) {
        PaymentRecordEntity record = paymentRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"PENDING".equals(record.getStatus())) {
            return Result.error(StatusCode.FAIL, "订单状态异常");
        }

        // 更新订单状态
        record.setStatus("SUCCESS");
        record.setPayTime(LocalDateTime.now());
        paymentRecordRepository.save(record);

        // 更新账户余额
        StudentAccountEntity account = accountRepository.findByStudentId(record.getStudentId())
                .orElseGet(() -> {
                    StudentAccountEntity newAccount = new StudentAccountEntity();
                    newAccount.setStudentId(record.getStudentId());
                    return newAccount;
                });
        account.setBalance(account.getBalance() + record.getAmount());
        accountRepository.save(account);

        return Result.success("支付成功，余额已更新");
    }

    // 取消支付
    public Result<String> cancelPayment(Long recordId) {
        PaymentRecordEntity record = paymentRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"PENDING".equals(record.getStatus())) {
            return Result.error(StatusCode.FAIL, "订单状态异常");
        }

        record.setStatus("FAILED");
        paymentRecordRepository.save(record);
        return Result.success("已取消支付");
    }

    // 新增退款方法
    @Transactional
    public Result<String> refund(Long recordId) {
        PaymentRecordEntity record = paymentRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));

        if (!"SUCCESS".equals(record.getStatus())) {
            return Result.error(StatusCode.FAIL, "只有已支付的订单可以退款");
        }

        // 更新支付记录状态
        record.setStatus("REFUNDED");
        record.setRefundTime(LocalDateTime.now());
        paymentRecordRepository.save(record);

        // 退还金额到学员账户
        StudentAccountEntity account = accountRepository.findByStudentId(record.getStudentId())
                .orElseThrow(() -> new RuntimeException("学员账户不存在"));
        account.setBalance(account.getBalance() - record.getAmount());  // 减去已支付金额（退款）
        accountRepository.save(account);

        return Result.success("退款成功");
    }
}
