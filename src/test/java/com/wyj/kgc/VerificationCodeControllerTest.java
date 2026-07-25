package com.wyj.kgc;

import com.wyj.kgc.controller.VerificationCodeController;
import com.wyj.kgc.config.VerificationCodeProperties;
import com.wyj.kgc.entity.VerificationCode;
import com.wyj.kgc.service.EmailVerificationCodeSender;
import com.wyj.kgc.service.TencentSmsVerificationCodeSender;
import com.wyj.kgc.service.VerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VerificationCodeControllerTest {

    private VerificationCodeService verificationCodeService;
    private EmailVerificationCodeSender emailSender;
    private TencentSmsVerificationCodeSender smsSender;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        verificationCodeService = mock(VerificationCodeService.class);
        emailSender = mock(EmailVerificationCodeSender.class);
        smsSender = mock(TencentSmsVerificationCodeSender.class);
        VerificationCodeProperties properties = new VerificationCodeProperties();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new VerificationCodeController(verificationCodeService, emailSender, smsSender, properties))
                .build();
    }

    @Test
    void sendEmailCodeReturnsOk() throws Exception {
        VerificationCode issued = new VerificationCode();
        issued.setCode("123456");
        when(verificationCodeService.issue("email", "student@example.com")).thenReturn(issued);

        mockMvc.perform(post("/api/v1/auth/verification-code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"channel":"email","target":"student@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification code sent."));

        verify(emailSender).send("student@example.com", "123456");
    }

    @Test
    void verifyCodeReturnsOk() throws Exception {
        when(verificationCodeService.verify(anyString(), anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/verification-code/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"channel":"email","target":"student@example.com","code":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void sendSmsCodeReturnsBadRequestWhenSmsDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/verification-code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"channel":"sms","target":"13800138000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("SMS verification is not enabled."));
    }
}
