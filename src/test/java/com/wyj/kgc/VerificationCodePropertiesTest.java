package com.wyj.kgc;

import com.wyj.kgc.config.VerificationCodeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "verification-code.ttl-seconds=300",
                    "verification-code.cooldown-seconds=60"
            );

    @Test
    void bindsVerificationCodeSettings() {
        contextRunner.run(context -> {
            VerificationCodeProperties properties = context.getBean(VerificationCodeProperties.class);

            assertThat(properties.getTtlSeconds()).isEqualTo(300);
            assertThat(properties.getCooldownSeconds()).isEqualTo(60);
        });
    }

    @EnableConfigurationProperties(VerificationCodeProperties.class)
    static class TestConfiguration {
    }
}
