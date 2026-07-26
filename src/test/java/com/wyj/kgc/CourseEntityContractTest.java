package com.wyj.kgc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CourseEntityContractTest {

    @Test
    void courseEntityDeclaresCoreFields() {
        assertDoesNotThrow(() -> {
            Class<?> courseClass = Class.forName("com.wyj.kgc.entity.Course");
            courseClass.getDeclaredField("id");
            courseClass.getDeclaredField("name");
            courseClass.getDeclaredField("description");
            courseClass.getDeclaredField("owner");
            courseClass.getDeclaredField("createdAt");
            courseClass.getDeclaredField("updatedAt");
        });
    }
}
