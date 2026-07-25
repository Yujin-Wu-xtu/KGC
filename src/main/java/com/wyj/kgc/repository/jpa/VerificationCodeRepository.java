package com.wyj.kgc.repository.jpa;

import com.wyj.kgc.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findTopByChannelAndTargetOrderByCreatedAtDesc(String channel, String target);

    Optional<VerificationCode> findFirstByChannelAndTargetAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String channel, String target, LocalDateTime now);

    long countByChannelAndTargetAndCreatedAtAfter(String channel, String target, LocalDateTime startOfDay);
}
