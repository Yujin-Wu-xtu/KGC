package com.wyj.kgc.controller;

import com.wyj.kgc.entity.ResourceFile;
import com.wyj.kgc.entity.User;
import com.wyj.kgc.service.FileStorageService;
import com.wyj.kgc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 负责处理所有文件相关的API请求
 * 对应您申报书中的 "图6 生成页"
 */
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*") // 允许任何情况下的跨域请求
@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @Autowired
    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<ResourceFile>> listFiles(
            @RequestParam(value = "courseId", required = false) Long courseId) {
        return ResponseEntity.ok(fileStorageService.listFilesByCourse(courseId));
    }

    @PostMapping("/upload")
    public ResponseEntity<ResourceFile> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("courseId") Long courseId,
            org.springframework.security.core.Authentication authentication) {
        
        // 兼容测试模式：若没有 Token 传进来（即 authentication==null），直接绕过找用户这一步
        User currentUser = null;
        if (authentication != null) {
            String username = authentication.getName();
            currentUser = userService.getUserByUsername(username);
        }

        // 把文件存下来。如果 currentUser=null，数据库的用户外键也应该允许为空，或需要注意一下报错
        ResourceFile savedFile = fileStorageService.storeFile(file, courseId, currentUser);

        return ResponseEntity.ok(savedFile);
    }

    // --- 6. 新增：注入 KnowledgeGraphService ---
    @Autowired
    private com.wyj.kgc.service.kg.KnowledgeGraphService knowledgeGraphService;

    /**
     * 触发知识提取的接口
     * POST /api/v1/files/{fileId}/parse
     * 返回 DeepSeek 提取的知识图谱 JSON 数据
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable("fileId") Long fileId,
                                           org.springframework.security.core.Authentication authentication) {
        User currentUser = null;
        if (authentication != null) {
            String username = authentication.getName();
            currentUser = userService.getUserByUsername(username);
        }

        fileStorageService.deleteFile(fileId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{fileId}/parse")
    public ResponseEntity<String> parseFile(@PathVariable("fileId") Long fileId) {
        try {
            String graphJson = knowledgeGraphService.parseAndSave(fileId);
            return ResponseEntity.ok(graphJson);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error parsing file: " + e.getMessage());
        }
    }
}
