package com.wyj.kgc;

import com.wyj.kgc.config.VerificationCodeProperties;
import com.wyj.kgc.service.EmailVerificationCodeSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailVerificationCodeSenderTest {

    @Test
    void sendsSixDigitCodeByEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        VerificationCodeProperties properties = new VerificationCodeProperties();
        properties.getEmail().setFrom("noreply@example.com");
        EmailVerificationCodeSender sender = new EmailVerificationCodeSender(mailSender, properties,
                "sender@example.com");

        sender.send("student@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("noreply@example.com");
        assertThat(message.getTo()).containsExactly("student@example.com");
        assertThat(message.getSubject()).contains("verification");
        assertThat(message.getText()).contains("123456").contains("5 minutes");
    }

    @Test
    void usesInjectedMailUsernameWhenSenderAddressIsBlank() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        VerificationCodeProperties properties = new VerificationCodeProperties();
        EmailVerificationCodeSender sender = new EmailVerificationCodeSender(mailSender, properties,
                "sender@example.com");

        sender.send("student@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isEqualTo("sender@example.com");
    }
}
