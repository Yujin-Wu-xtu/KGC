package com.wyj.kgc;

import com.wyj.kgc.dto.CourseRequest;
import com.wyj.kgc.entity.Course;
import com.wyj.kgc.entity.User;
import com.wyj.kgc.repository.jpa.CourseRepository;
import com.wyj.kgc.service.CourseService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseServiceBehaviorTest {

    @Test
    void createCoursePersistsTrimmedData() {
        CourseRepository repository = mock(CourseRepository.class);
        CourseService service = new CourseService(repository);
        User owner = new User();
        owner.setId(7L);

        Course saved = new Course();
        saved.setId(11L);
        saved.setName("Data Structure");
        saved.setDescription("Core course");
        saved.setOwner(owner);
        when(repository.save(any(Course.class))).thenReturn(saved);

        CourseRequest request = new CourseRequest();
        request.setName("  Data Structure  ");
        request.setDescription("  Core course  ");

        Course result = service.createCourse(request, owner);

        assertEquals(11L, result.getId());
        assertEquals("Data Structure", result.getName());
        assertEquals("Core course", result.getDescription());
        assertEquals(owner, result.getOwner());
        verify(repository).save(any(Course.class));
    }

    @Test
    void listCoursesReturnsRepositoryOrder() {
        CourseRepository repository = mock(CourseRepository.class);
        CourseService service = new CourseService(repository);
        Course course = new Course();
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(course));

        assertEquals(List.of(course), service.listCourses());
    }

    @Test
    void getCourseRejectsMissingId() {
        CourseRepository repository = mock(CourseRepository.class);
        CourseService service = new CourseService(repository);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getCourse(99L));
    }
}
