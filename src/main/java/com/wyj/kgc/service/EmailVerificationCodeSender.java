package com.wyj.kgc.service;

import com.wyj.kgc.config.VerificationCodeProperties;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailVerificationCodeSender implements VerificationCodeSender {

    private final JavaMailSender mailSender;
    private final VerificationCodeProperties properties;
    private final String mailUsername;

    public EmailVerificationCodeSender(JavaMailSender mailSender, VerificationCodeProperties properties,
            @Value("${spring.mail.username}") String mailUsername) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.mailUsername = mailUsername;
    }

    @Override
    public void send(String target, String code) {
        String senderAddress = resolveSenderAddress();
        String targetAddress = normalizeAddress(target, "Verification target email address is invalid.");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderAddress);
        message.setTo(targetAddress);
        message.setSubject("KGC verification code");
        message.setText("Your KGC verification code is " + code + ". It expires in "
                + properties.getTtlSeconds() / 60 + " minutes.");
        mailSender.send(message);
    }

    private String resolveSenderAddress() {
        String configuredFrom = properties.getEmail() == null ? null : properties.getEmail().getFrom();
        if (StringUtils.hasText(configuredFrom)) {
            return normalizeAddress(configuredFrom, "Mail sender address is invalid.");
        }
        if (StringUtils.hasText(mailUsername)) {
            return normalizeAddress(mailUsername, "Mail username is invalid for sender address.");
        }
        throw new IllegalStateException("Mail sender address is not configured.");
    }

    private String normalizeAddress(String address, String invalidMessage) {
        if (!StringUtils.hasText(address)) {
            throw new IllegalArgumentException(invalidMessage);
        }
        String trimmedAddress = address.trim();
        try {
            InternetAddress internetAddress = new InternetAddress(trimmedAddress, true);
            internetAddress.validate();
            return internetAddress.getAddress();
        } catch (AddressException e) {
            throw new IllegalArgumentException(invalidMessage, e);
        }
    }
}
