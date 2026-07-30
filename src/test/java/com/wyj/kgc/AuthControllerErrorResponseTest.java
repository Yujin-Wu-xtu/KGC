package com.wyj.kgc;

import com.wyj.kgc.controller.AuthController;
import com.wyj.kgc.dto.RegisterRequest;
import com.wyj.kgc.entity.UserRole;
import com.wyj.kgc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerErrorResponseTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService)).build();
    }

    @Test
    void registerFailureReturnsJsonMessage() throws Exception {
        when(userService.registerUser(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("This email address is already registered."));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@example.com",
                                  "emailCode": "123456",
                                  "password": "secret123",
                                  "role": "ROLE_STUDENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("This email address is already registered."));
    }

    @Test
    void loginFailureReturnsJsonMessage() throws Exception {
        when(userService.login("student@example.com", "wrong-password"))
                .thenThrow(new IllegalArgumentException("Invalid account or password."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"student@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid account or password."));
    }
}
