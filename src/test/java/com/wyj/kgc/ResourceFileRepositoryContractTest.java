package com.wyj.kgc;

import com.wyj.kgc.repository.jpa.ResourceFileRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceFileRepositoryContractTest {

    @Test
    void repositoryCanQueryResourcesByCourseId() {
        assertDoesNotThrow(() -> {
            var method = ResourceFileRepository.class.getMethod("findByCourseIdOrderByCreatedAtDesc", Long.class);
            assertEquals(List.class, method.getReturnType());
        });
    }
}
