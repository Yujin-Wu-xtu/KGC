package com.wyj.kgc.repository.jpa;

import com.wyj.kgc.entity.Course;
import com.wyj.kgc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByOrderByCreatedAtDesc();
    List<Course> findByPublishedTrueOrderByCreatedAtDesc();
    List<Course> findByOwnerAndPublishedTrueOrderByCreatedAtDesc(User owner);
}
