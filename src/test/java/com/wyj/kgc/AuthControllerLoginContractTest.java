package com.wyj.kgc;

import com.wyj.kgc.controller.AuthController;
import com.wyj.kgc.dto.LoginRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthControllerLoginContractTest {

    @Test
    void loginEndpointAcceptsDedicatedLoginRequest() {
        assertTrue(Arrays.stream(AuthController.class.getMethods())
                .anyMatch(method -> method.getName().equals("login")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[]{LoginRequest.class})));
    }
}
