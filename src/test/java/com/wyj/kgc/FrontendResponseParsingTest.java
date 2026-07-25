package com.wyj.kgc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendResponseParsingTest {

    @Test
    void authPagesDoNotReadFetchResponseBodyTwice() throws Exception {
        assertDoesNotUseJsonCatchText("src/main/resources/static/register.html");
        assertDoesNotUseJsonCatchText("src/main/resources/static/login.html");
    }

    private void assertDoesNotUseJsonCatchText(String file) throws Exception {
        String html = Files.readString(Path.of(file));

        assertThat(html).doesNotContain("response.json().catch(async () => ({ message: await response.text() }))");
        assertThat(html).contains("parseResponsePayload(response)");
    }
}
