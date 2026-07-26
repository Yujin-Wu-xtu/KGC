package com.wyj.kgc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoursesPageBackendApiTest {

    @Test
    void coursesPageUsesBackendCourseApi() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/static/courses.html"));

        assertTrue(html.contains("/api/v1/courses"));
        assertTrue(html.contains("fetch("));
        assertFalse(html.contains("localStorage.setItem('kgc_courses'"));
    }
}
