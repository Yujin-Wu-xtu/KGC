package com.wyj.kgc;

import com.wyj.kgc.dto.RegisterRequest;
import com.wyj.kgc.entity.User;
import com.wyj.kgc.entity.UserRole;
import com.wyj.kgc.repository.jpa.UserRepository;
import com.wyj.kgc.security.JwtTokenProvider;
import com.wyj.kgc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceContactAuthenticationTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private UserService userService;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        userService = new UserService(userRepository, passwordEncoder);

        Field tokenProviderField = UserService.class.getDeclaredField("jwtTokenProvider");
        tokenProviderField.setAccessible(true);
        tokenProviderField.set(userService, jwtTokenProvider);
    }

    @Test
    void registersWithNormalizedEmailAndPhone() {
        RegisterRequest request = registrationRequest(" Teacher@Example.COM ", "138-0013-8000");
        when(userRepository.findByUsername("teacher@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("teacher@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = userService.registerUser(request);

        assertEquals("teacher@example.com", registered.getUsername());
        assertEquals("teacher@example.com", registered.getEmail());
        assertEquals("13800138000", registered.getPhone());
        assertEquals("hashed-password", registered.getPassword());
        assertEquals(UserRole.ROLE_TEACHER, registered.getRole());
    }

    @Test
    void rejectsRegistrationWithoutEmailOrPhone() {
        RegisterRequest request = registrationRequest(null, null);

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));
    }

    @Test
    void rejectsDuplicateEmail() {
        RegisterRequest request = registrationRequest("teacher@example.com", null);
        when(userRepository.findByEmail("teacher@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));
    }

    @Test
    void logsInWithEmail() {
        User user = existingUser("teacher_001", "teacher@example.com", "13800138000");
        when(userRepository.findByUsername("teacher@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("teacher@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtTokenProvider.createToken("teacher_001")).thenReturn("email-token");

        String token = assertDoesNotThrow(() -> userService.login("teacher@example.com", "password123"));

        assertEquals("email-token", token);
    }

    @Test
    void logsInWithPhone() {
        User user = existingUser("student_001", null, "13800138000");
        when(userRepository.findByUsername("13800138000")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtTokenProvider.createToken("student_001")).thenReturn("phone-token");

        String token = assertDoesNotThrow(() -> userService.login("13800138000", "password123"));

        assertEquals("phone-token", token);
    }

    private RegisterRequest registrationRequest(String email, String phone) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPhone(phone);
        request.setPassword("password123");
        request.setRole(UserRole.ROLE_TEACHER);
        return request;
    }

    private User existingUser(String username, String email, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword("hashed-password");
        user.setRole(UserRole.ROLE_TEACHER);
        return user;
    }
}
