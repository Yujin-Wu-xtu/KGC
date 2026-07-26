package com.wyj.kgc;

import com.wyj.kgc.controller.CourseController;
import com.wyj.kgc.entity.Course;
import com.wyj.kgc.service.CourseService;
import com.wyj.kgc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseControllerBehaviorTest {

    private CourseService courseService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        courseService = mock(CourseService.class);
        UserService userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseController(courseService, userService)).build();
    }

    @Test
    void listCoursesReturnsCourses() throws Exception {
        Course course = new Course();
        course.setId(1L);
        course.setName("Data Structure");
        when(courseService.listCourses()).thenReturn(List.of(course));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Data Structure"));
    }

    @Test
    void createCourseReturnsCreatedCourse() throws Exception {
        Course course = new Course();
        course.setId(2L);
        course.setName("Computer Networks");
        when(courseService.createCourse(any(), isNull())).thenReturn(course);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Computer Networks","description":"Network basics"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Computer Networks"));
    }

    @Test
    void deleteCourseReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/3"))
                .andExpect(status().isNoContent());
    }
}
