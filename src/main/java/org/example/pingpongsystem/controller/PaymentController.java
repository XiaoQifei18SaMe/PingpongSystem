// PingpongSystem/src/main/java/org/example/pingpongsystem/controller/PaymentController.java
package org.example.pingpongsystem.controller;

import org.example.pingpongsystem.entity.PaymentRecordEntity;
import org.example.pingpongsystem.service.PaymentService;
import org.example.pingpongsystem.utility.Result;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/payment")
@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 获取余额
    @GetMapping("/balance")
    public Result<Double> getBalance(@RequestParam Long studentId) {
        return paymentService.getBalance(studentId);
    }

    // 创建支付订单
    @PostMapping("/create")
    public Result<PaymentRecordEntity> createPayment(
            @RequestParam Long studentId,
            @RequestParam Double amount,
            @RequestParam String method) {
        return paymentService.createPayment(studentId, amount, method);
    }

    // 确认支付
    @PostMapping("/confirm")
    public Result<String> confirmPayment(@RequestParam Long recordId) {
        return paymentService.confirmPayment(recordId);
    }

    // 取消支付
    @PostMapping("/cancel")
    public Result<String> cancelPayment(@RequestParam Long recordId) {
        return paymentService.cancelPayment(recordId);
    }

    // 新增：获取交易记录
    @GetMapping("/records")
    public Result<Page<PaymentRecordEntity>> getPaymentRecords(
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method) {
        return paymentService.getPaymentRecords(studentId, page, size, status, method);
    }
}