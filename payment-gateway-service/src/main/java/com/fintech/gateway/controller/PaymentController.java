package com.fintech.gateway.controller;

import com.fintech.common.dto.PaymentRequestDTO;
import com.fintech.common.dto.PaymentResponseDTO;
import com.fintech.gateway.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> processPayment(@RequestBody PaymentRequestDTO request) {
        log.info("Received payment request: payer={}, amount={}", 
                request.payerId(), request.amount());
        
        PaymentResponseDTO response = paymentService.processPayment(request);
        
        return switch (response.status()) {
            case "AUTHORIZED" -> ResponseEntity.ok(response);
            case "REJECTED" -> ResponseEntity.badRequest().body(response);
            default -> ResponseEntity.internalServerError().body(response);
        };
    }
}
