package com.wyj.kgc;

import com.wyj.kgc.config.VerificationCodeProperties;
import com.wyj.kgc.entity.VerificationCode;
import com.wyj.kgc.repository.jpa.VerificationCodeRepository;
import com.wyj.kgc.service.VerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationCodeServiceTest {

    private VerificationCodeRepository repository;
    private VerificationCodeService service;

    @BeforeEach
    void setUp() {
        repository = mock(VerificationCodeRepository.class);
        VerificationCodeProperties properties = new VerificationCodeProperties();
        properties.setTtlSeconds(300);
        properties.setCooldownSeconds(60);
        properties.setDailyLimit(10);
        Clock clock = Clock.fixed(Instant.parse("2026-07-25T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new VerificationCodeService(repository, properties, clock);
    }

    @Test
    void createsAndValidatesActiveEmailCode() {
        when(repository.findTopByChannelAndTargetOrderByCreatedAtDesc("email", "student@example.com"))
                .thenReturn(Optional.empty());
        when(repository.countByChannelAndTargetAndCreatedAtAfter(eq("email"), eq("student@example.com"), any()))
                .thenReturn(0L);
        when(repository.save(any(VerificationCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationCode issued = service.issue("email", "student@example.com");
        when(repository.findFirstByChannelAndTargetAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("email"), eq("student@example.com"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(issued));

        assertThat(issued.getCode()).matches("\\d{6}");
        assertThat(issued.getCodeHash()).doesNotContain(issued.getCode());
        assertThat(service.verify("email", "student@example.com", issued.getCode())).isTrue();
        assertThat(issued.getUsedAt()).isNotNull();
        verify(repository, times(2)).save(issued);
    }

    @Test
    void rejectsIssueDuringCooldownWindow() {
        VerificationCode recent = new VerificationCode();
        recent.setChannel("sms");
        recent.setTarget("13800138000");
        recent.setLastSentAt(LocalDateTime.of(2026, 7, 25, 9, 59, 30));

        when(repository.findTopByChannelAndTargetOrderByCreatedAtDesc("sms", "13800138000"))
                .thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> service.issue("sms", "13800138000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Please wait");
    }
}
