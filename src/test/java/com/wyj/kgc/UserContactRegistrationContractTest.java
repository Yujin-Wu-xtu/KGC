package com.wyj.kgc;

import com.wyj.kgc.dto.RegisterRequest;
import com.wyj.kgc.entity.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserContactRegistrationContractTest {

    @Test
    void userAndRegistrationRequestExposeEmailAndPhoneFields() {
        assertTrue(fieldNames(User.class).containsAll(Set.of("email", "phone")));
        assertTrue(methodNames(RegisterRequest.class).containsAll(Set.of(
                "getEmail", "setEmail", "getPhone", "setPhone"
        )));
    }

    private Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }
}
