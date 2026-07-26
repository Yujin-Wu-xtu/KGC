package com.wyj.kgc.repository.jpa;

import com.wyj.kgc.entity.ResourceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceFileRepository extends JpaRepository<ResourceFile, Long> {
    List<ResourceFile> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    List<ResourceFile> findAllByOrderByCreatedAtDesc();
}
