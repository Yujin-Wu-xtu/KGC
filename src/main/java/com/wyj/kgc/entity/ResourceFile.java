package com.wyj.kgc.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资源文件实体类
 * 对应您申报书中的 "课程素材文件"
 * 这将自动在 MySQL 中映射为一张名为 'resource_files' 的表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "resource_files")
public class ResourceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName; // 原始文件名, e.g., "数据结构.pdf"

    @Column(nullable = false)
    private String fileType; // 文件MIME类型, e.g., "application/pdf"

    @Column(nullable = false, length = 1024)
    private String filePath; // 文件在F盘上存储的绝对路径

    // 这个ID用来关联到未来的 "Course" (课程) 表
    // 目前我们先简单地把它当做一个数字存起来
    @Column(nullable = false)
    private Long courseId;

    /**
     * 声明一个 "多对一" 的关系：
     * 多个 ResourceFile 实体会对应一个 User 实体。
     * fetch = FetchType.LAZY 是性能优化，告诉JPA在加载ResourceFile时，
     * 先不要去加载关联的User对象，除非我们明确调用 .getUser()。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    /**
     * 告诉 JPA 在 "resource_files" 表中创建一个叫 "user_id" 的外键列
     * 它会关联到 "users" 表的主键。
     */
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user; // 在 Java 对象中，我们直接持有 User 对象的引用

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}