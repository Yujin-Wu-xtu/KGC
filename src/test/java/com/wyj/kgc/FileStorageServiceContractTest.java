package com.wyj.kgc;

import com.wyj.kgc.service.FileStorageService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileStorageServiceContractTest {

    @Test
    void serviceCanListFilesByCourse() {
        assertDoesNotThrow(() -> {
            var method = FileStorageService.class.getMethod("listFilesByCourse", Long.class);
            assertEquals(List.class, method.getReturnType());
        });
    }
}
