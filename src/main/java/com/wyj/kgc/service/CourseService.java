package com.wyj.kgc.service;

import com.wyj.kgc.dto.CourseRequest;
import com.wyj.kgc.entity.Course;
import com.wyj.kgc.entity.User;
import com.wyj.kgc.repository.jpa.CourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> listCourses() {
        return courseRepository.findAllByOrderByCreatedAtDesc();
    }

    public Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course does not exist."));
    }

    public Course createCourse(CourseRequest request, User owner) {
        if (request == null) {
            throw new IllegalArgumentException("Course request must not be empty.");
        }

        String name = normalizeRequiredText(request.getName(), "Course name is required.");
        String description = normalizeOptionalText(request.getDescription());

        Course course = new Course();
        course.setName(name);
        course.setDescription(description);
        course.setOwner(owner);
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, CourseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Course request must not be empty.");
        }

        Course course = getCourse(id);
        course.setName(normalizeRequiredText(request.getName(), "Course name is required."));
        course.setDescription(normalizeOptionalText(request.getDescription()));
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        Course course = getCourse(id);
        courseRepository.delete(course);
    }

    public Course setCoursePublishedStatus(Long id, boolean published) {
        Course course = getCourse(id);
        course.setPublished(published);
        return courseRepository.save(course);
    }

    public List<Course> listPublishedCourses() {
        return courseRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
