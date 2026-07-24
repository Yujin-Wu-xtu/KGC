package com.wyj.kgc;

import com.wyj.kgc.dto.RegisterRequest;
import com.wyj.kgc.repository.jpa.UserRepository;
import com.wyj.kgc.service.UserService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserContactAuthenticationApiTest {

    @Test
    void authenticationApiExposesContactRegistrationAndLookupMethods() {
        assertTrue(hasMethod(UserService.class, "registerUser", RegisterRequest.class));
        assertTrue(hasMethod(UserRepository.class, "findByEmail", String.class));
        assertTrue(hasMethod(UserRepository.class, "findByPhone", String.class));
    }

    private boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        return Arrays.stream(type.getMethods())
                .anyMatch(method -> method.getName().equals(name)
                        && Arrays.equals(method.getParameterTypes(), parameterTypes));
    }
}
