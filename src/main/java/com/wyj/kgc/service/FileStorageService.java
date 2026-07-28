package com.wyj.kgc.service;

import com.wyj.kgc.entity.ResourceFile;
import com.wyj.kgc.entity.User;
import com.wyj.kgc.repository.jpa.CourseRepository;
import com.wyj.kgc.repository.jpa.ResourceFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 业务逻辑层 (Service) - 负责处理文件存储
 * 既包括物理文件系统 (存到F盘)
 * 也包括数据库 (存入MySQL)
 */
@Service // 1. 声明这是一个 Spring 的 Service "组件"
public class FileStorageService {

    // 2. 注入 ResourceFile 的数据库操作接口
    private final ResourceFileRepository resourceFileRepository;
    private final CourseRepository courseRepository;

    // 3. 存储文件的物理路径 (从 application.properties 读取)
    private final Path fileStorageLocation;

    @Autowired // 4. 构造函数注入 (Spring Boot 推荐的方式)
    public FileStorageService(ResourceFileRepository resourceFileRepository,
                              CourseRepository courseRepository,
                              @Value("${file.upload-dir}") String uploadDir) {

        this.resourceFileRepository = resourceFileRepository;
        this.courseRepository = courseRepository;

        // 5. 将字符串路径 "F:/kgc_uploads/" 转换为 Path 对象
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        // 6. 启动时检查：如果目录不存在，就创建它
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            // 如果创建失败，抛出运行时异常，程序会启动失败
            throw new RuntimeException("无法创建用于存储文件的目录！", ex);
        }
    }

    /**
     * 核心方法：存储上传的文件
     * @param file 前端传来的文件
     * @param courseId 文件所属的课程ID
     * @return 已经存入数据库的 ResourceFile 实体
     */
    // 📋 FileStorageService.java

// 👇 注意看参数列表最后，加上了 User currentUser
    public ResourceFile storeFile(MultipartFile file, Long courseId, User currentUser) {
        // 1. 获取文件名
        String originalFileName = file.getOriginalFilename();

        // 防止文件名为空 (防御性编程)
        if (originalFileName == null) {
            throw new RuntimeException("文件名不能为空");
        }

        // 2. 确定路径
        Path targetLocation = this.fileStorageLocation.resolve(originalFileName);

        try {
            // 3. 复制文件到硬盘
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 4. 构建数据库实体
            ResourceFile resourceFile = new ResourceFile();
            resourceFile.setFileName(originalFileName);
            resourceFile.setFileType(file.getContentType());
            resourceFile.setFilePath(targetLocation.toString());

            // 查找课程，如果找不到则抛出异常
            com.wyj.kgc.entity.Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("关联的课程不存在, id=" + courseId));
            resourceFile.setCourse(course);

            // 5. 关键：关联用户
            resourceFile.setUser(currentUser);

            // 6. 保存并返回
            return resourceFileRepository.save(resourceFile);

        } catch (IOException ex) {
            throw new RuntimeException("无法存储文件 " + originalFileName + ". 请重试!", ex);
        }
    }

    public List<ResourceFile> listFilesByCourse(Long courseId) {
        if (courseId == null) {
            return resourceFileRepository.findAllByOrderByCreatedAtDesc();
        }
        return resourceFileRepository.findByCourse_IdOrderByCreatedAtDesc(courseId);
    }

    /**
     * 删除文件及其数据库记录
     */
    public void deleteFile(Long fileId, User currentUser) {
        ResourceFile resourceFile = resourceFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("找不到对应的资源文件, id=" + fileId));

        // 简单的权限校验：仅允许所有者删除（如果没有 currentUser 则是测试绕过模式，直接删）
        if (currentUser != null && resourceFile.getUser() != null) {
            if (!resourceFile.getUser().getId().equals(currentUser.getId())) {
                throw new SecurityException("没有权限删除此文件！");
            }
        }

        // 删除物理文件
        try {
            Path targetLocation = Paths.get(resourceFile.getFilePath());
            Files.deleteIfExists(targetLocation);
        } catch (IOException e) {
            throw new RuntimeException("删除物理文件失败", e);
        }

        // 删除数据库记录
        resourceFileRepository.delete(resourceFile);
    }
}
