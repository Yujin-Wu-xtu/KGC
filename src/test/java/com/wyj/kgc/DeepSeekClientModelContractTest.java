package com.wyj.kgc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepSeekClientModelContractTest {

    @Test
    void deepSeekClientUsesSupportedDefaultModel() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/wyj/kgc/service/DeepSeekClient.java"));
        String applicationProperties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertTrue(source.contains("deepseek.model"));
        assertTrue(source.contains("requestBodyNode.put(\"model\", deepSeekModel);"));
        assertTrue(applicationProperties.contains("deepseek.model=${DEEPSEEK_MODEL:deepseek-v4-pro}"));
    }
}
