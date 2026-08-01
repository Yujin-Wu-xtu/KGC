package com.wyj.kgc.controller;

import com.wyj.kgc.repository.jpa.CourseRepository;
import com.wyj.kgc.repository.jpa.ResourceFileRepository;
import com.wyj.kgc.repository.neo4j.KnowledgeNodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统状态控制器
 * 供教师端设置页展示真实的运行状态：DeepSeek 模型配置、Neo4j 连通性、平台数据统计。
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final Neo4jClient neo4jClient;
    private final CourseRepository courseRepository;
    private final ResourceFileRepository resourceFileRepository;
    private final KnowledgeNodeRepository knowledgeNodeRepository;

    @Value("${deepseek.model:deepseek-v4-pro}")
    private String deepSeekModel;

    @Value("${deepseek.api.key:}")
    private String deepSeekApiKey;

    @Value("${spring.neo4j.uri:}")
    private String neo4jUri;

    public SystemController(Neo4jClient neo4jClient,
                            CourseRepository courseRepository,
                            ResourceFileRepository resourceFileRepository,
                            KnowledgeNodeRepository knowledgeNodeRepository) {
        this.neo4jClient = neo4jClient;
        this.courseRepository = courseRepository;
        this.resourceFileRepository = resourceFileRepository;
        this.knowledgeNodeRepository = knowledgeNodeRepository;
    }

    /** 当前 DeepSeek 模型名（protected 便于测试覆盖） */
    protected String deepSeekModel() {
        return deepSeekModel;
    }

    /** DeepSeek API Key 是否存在（protected 便于测试覆盖） */
    protected String deepSeekApiKey() {
        return deepSeekApiKey;
    }

    /** Neo4j 连接 URI（protected 便于测试覆盖） */
    protected String neo4jUri() {
        return neo4jUri;
    }

    /**
     * GET /api/v1/system/status — 系统运行状态概览
     * 返回 DeepSeek 模型配置、Neo4j 连通状态、平台数据统计。
     * 注意：绝不返回 API Key 本身，仅返回是否已配置。
     */
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        Map<String, Object> result = new LinkedHashMap<>();

        // --- DeepSeek 配置 ---
        Map<String, Object> deepseek = new LinkedHashMap<>();
        deepseek.put("model", deepSeekModel());
        deepseek.put("apiKeyConfigured", deepSeekApiKey() != null && !deepSeekApiKey().isBlank());
        result.put("deepseek", deepseek);

        // --- Neo4j 连通状态（真实测试连接） ---
        Map<String, Object> neo4j = new LinkedHashMap<>();
        neo4j.put("uri", neo4jUri());
        boolean neo4jConnected = false;
        try {
            neo4jClient.query("RETURN 1 AS ok").fetch().all();
            neo4jConnected = true;
        } catch (Exception e) {
            neo4jConnected = false;
        }
        neo4j.put("connected", neo4jConnected);

        // 节点数统计（Neo4j 断开时返回 null）
        Long nodeCount = null;
        if (neo4jConnected) {
            try {
                nodeCount = knowledgeNodeRepository.count();
            } catch (Exception e) {
                nodeCount = null;
            }
        }
        neo4j.put("nodeCount", nodeCount);
        result.put("neo4j", neo4j);

        // --- 平台数据统计（MySQL） ---
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("courseCount", courseRepository.count());
        stats.put("fileCount", resourceFileRepository.count());
        result.put("stats", stats);

        return ResponseEntity.ok(result);
    }
}
