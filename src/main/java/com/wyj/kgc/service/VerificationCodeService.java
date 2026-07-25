package com.wyj.kgc.service;

import com.wyj.kgc.config.VerificationCodeProperties;
import com.wyj.kgc.entity.VerificationCode;
import com.wyj.kgc.repository.jpa.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class VerificationCodeService {

    private final VerificationCodeRepository repository;
    private final VerificationCodeProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public VerificationCodeService(VerificationCodeRepository repository, VerificationCodeProperties properties) {
        this(repository, properties, Clock.systemDefaultZone());
    }

    public VerificationCodeService(VerificationCodeRepository repository, VerificationCodeProperties properties,
            Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(transactionManager = "transactionManager")
    public VerificationCode issue(String channel, String target) {
        String normalizedChannel = normalizeChannel(channel);
        String normalizedTarget = normalizeTarget(target);
        LocalDateTime now = now();

        repository.findTopByChannelAndTargetOrderByCreatedAtDesc(normalizedChannel, normalizedTarget)
                .ifPresent(last -> ensureCooldownPassed(last, now));

        LocalDateTime startOfDay = LocalDate.now(clock).atStartOfDay();
        long sentToday = repository.countByChannelAndTargetAndCreatedAtAfter(
                normalizedChannel, normalizedTarget, startOfDay);
        if (sentToday >= properties.getDailyLimit()) {
            throw new IllegalStateException("Verification code daily limit exceeded.");
        }

        String plainCode = generateSixDigitCode();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setChannel(normalizedChannel);
        verificationCode.setTarget(normalizedTarget);
        verificationCode.setCode(plainCode);
        verificationCode.setCodeHash(hash(normalizedChannel, normalizedTarget, plainCode));
        verificationCode.setExpiresAt(now.plusSeconds(properties.getTtlSeconds()));
        verificationCode.setLastSentAt(now);
        verificationCode.setSendCountToday((int) sentToday + 1);
        verificationCode.setCreatedAt(now);
        verificationCode.setUpdatedAt(now);
        return repository.save(verificationCode);
    }

    @Transactional(transactionManager = "transactionManager")
    public boolean verify(String channel, String target, String code) {
        String normalizedChannel = normalizeChannel(channel);
        String normalizedTarget = normalizeTarget(target);
        String normalizedCode = normalizeCode(code);
        LocalDateTime now = now();

        VerificationCode verificationCode = repository
                .findFirstByChannelAndTargetAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        normalizedChannel, normalizedTarget, now)
                .orElseThrow(() -> new IllegalArgumentException("Verification code is invalid or expired."));

        String expectedHash = hash(normalizedChannel, normalizedTarget, normalizedCode);
        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8),
                verificationCode.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Verification code is invalid or expired.");
        }

        markUsed(verificationCode);
        return true;
    }

    @Transactional(transactionManager = "transactionManager")
    public void markUsed(VerificationCode verificationCode) {
        verificationCode.setUsedAt(now());
        verificationCode.setUpdatedAt(now());
        repository.save(verificationCode);
    }

    private void ensureCooldownPassed(VerificationCode last, LocalDateTime now) {
        LocalDateTime nextAllowedAt = last.getLastSentAt().plusSeconds(properties.getCooldownSeconds());
        if (now.isBefore(nextAllowedAt)) {
            throw new IllegalStateException("Please wait before requesting another verification code.");
        }
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification channel is required.");
        }
        String normalized = channel.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("email") && !normalized.equals("sms")) {
            throw new IllegalArgumentException("Unsupported verification channel.");
        }
        return normalized;
    }

    private String normalizeTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification target is required.");
        }
        return target.trim();
    }

    private String normalizeCode(String code) {
        if (code == null || !code.trim().matches("\\d{6}")) {
            throw new IllegalArgumentException("Verification code is invalid or expired.");
        }
        return code.trim();
    }

    private String generateSixDigitCode() {
        return String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    }

    private String hash(String channel, String target, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((channel + ":" + target + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
