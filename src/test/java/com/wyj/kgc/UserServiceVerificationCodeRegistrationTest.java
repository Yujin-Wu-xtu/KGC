package com.wyj.kgc;

import com.wyj.kgc.dto.RegisterRequest;
import com.wyj.kgc.entity.User;
import com.wyj.kgc.entity.UserRole;
import com.wyj.kgc.repository.jpa.UserRepository;
import com.wyj.kgc.service.UserService;
import com.wyj.kgc.service.VerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceVerificationCodeRegistrationTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private VerificationCodeService verificationCodeService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        verificationCodeService = mock(VerificationCodeService.class);
        userService = new UserService(userRepository, passwordEncoder, verificationCodeService);
    }

    @Test
    void registerRequiresMatchingEmailCode() {
        RegisterRequest request = registrationRequest();
        request.setEmail(" Student@Example.COM ");
        request.setEmailCode("123456");
        when(userRepository.findByUsername("student@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationCodeService.verify("email", "student@example.com", "123456")).thenReturn(true);

        assertDoesNotThrow(() -> userService.registerUser(request));

        verify(verificationCodeService).verify("email", "student@example.com", "123456");
    }

    @Test
    void registerRejectsMissingPhoneCode() {
        RegisterRequest request = registrationRequest();
        request.setPhone("138-0013-8000");

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mobile verification code");
    }

    private RegisterRequest registrationRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setPassword("password123");
        request.setRole(UserRole.ROLE_STUDENT);
        return request;
    }
}
