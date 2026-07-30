package com.wyj.kgc.controller;

import com.wyj.kgc.dto.VerificationCodeSendRequest;
import com.wyj.kgc.dto.VerificationCodeVerifyRequest;
import com.wyj.kgc.entity.VerificationCode;
import com.wyj.kgc.config.VerificationCodeProperties;
import com.wyj.kgc.service.EmailVerificationCodeSender;
import com.wyj.kgc.service.TencentSmsVerificationCodeSender;
import com.wyj.kgc.service.VerificationCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/verification-code")
public class VerificationCodeController {

    private final VerificationCodeService verificationCodeService;
    private final EmailVerificationCodeSender emailSender;
    private final TencentSmsVerificationCodeSender smsSender;
    private final VerificationCodeProperties properties;

    public VerificationCodeController(VerificationCodeService verificationCodeService,
            EmailVerificationCodeSender emailSender,
            TencentSmsVerificationCodeSender smsSender,
            VerificationCodeProperties properties) {
        this.verificationCodeService = verificationCodeService;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.properties = properties;
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody VerificationCodeSendRequest request) {
        try {
            String channel = normalizeChannel(request.getChannel());
            if ("sms".equals(channel) && !properties.getSms().isEnabled()) {
                throw new IllegalStateException("SMS verification is not enabled.");
            }
            VerificationCode verificationCode = verificationCodeService.issue(channel, request.getTarget());
            if ("email".equals(channel)) {
                emailSender.send(request.getTarget(), verificationCode.getCode());
            } else {
                smsSender.send(request.getTarget(), verificationCode.getCode());
            }
            return ResponseEntity.ok(Map.of("message", "Verification code sent."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerificationCodeVerifyRequest request) {
        try {
            boolean valid = verificationCodeService.verify(request.getChannel(), request.getTarget(), request.getCode());
            return ResponseEntity.ok(Map.of("valid", valid));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", e.getMessage()));
        }
    }

    private String normalizeChannel(String channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Verification channel is required.");
        }
        return channel.trim().toLowerCase(Locale.ROOT);
    }
}
