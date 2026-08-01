package com.wyj.kgc;

import com.wyj.kgc.controller.SystemController;
import com.wyj.kgc.repository.jpa.CourseRepository;
import com.wyj.kgc.repository.jpa.ResourceFileRepository;
import com.wyj.kgc.repository.neo4j.KnowledgeNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemStatusControllerTest {

    private Neo4jClient neo4jClient;
    private CourseRepository courseRepository;
    private ResourceFileRepository resourceFileRepository;
    private KnowledgeNodeRepository knowledgeNodeRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        neo4jClient = mock(Neo4jClient.class);
        courseRepository = mock(CourseRepository.class);
        resourceFileRepository = mock(ResourceFileRepository.class);
        knowledgeNodeRepository = mock(KnowledgeNodeRepository.class);

        // 使用测试专用子类注入固定配置值（避免反射修改私有字段）
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TestSystemController(neo4jClient, courseRepository, resourceFileRepository, knowledgeNodeRepository))
                .build();
    }

    @Test
    void statusReturnsDeepSeekAndNeo4jAndStats() throws Exception {
        // Neo4j 连接成功：query -> fetch -> all 返回空列表（连通即为成功）
        Neo4jClient.UnboundRunnableSpec querySpec = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.RecordFetchSpec<Map<String, Object>> fetchSpec = mock(Neo4jClient.RecordFetchSpec.class);
        when(querySpec.fetch()).thenReturn(fetchSpec);
        when(fetchSpec.all()).thenReturn(Collections.emptyList());
        when(neo4jClient.query(anyString())).thenReturn(querySpec);
        when(courseRepository.count()).thenReturn(3L);
        when(resourceFileRepository.count()).thenReturn(12L);
        when(knowledgeNodeRepository.count()).thenReturn(48L);

        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deepseek.model").value("deepseek-v4-pro"))
                .andExpect(jsonPath("$.deepseek.apiKeyConfigured").value(true))
                .andExpect(jsonPath("$.neo4j.connected").value(true))
                .andExpect(jsonPath("$.neo4j.nodeCount").value(48))
                .andExpect(jsonPath("$.stats.courseCount").value(3))
                .andExpect(jsonPath("$.stats.fileCount").value(12));
    }

    @Test
    void statusReportsNeo4jDisconnectedWhenQueryThrows() throws Exception {
        when(neo4jClient.query(anyString())).thenThrow(new RuntimeException("Neo4j down"));

        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.neo4j.connected").value(false))
                .andExpect(jsonPath("$.neo4j.nodeCount").doesNotExist());
    }

    @Test
    void statusDoesNotExposeApiKey() throws Exception {
        Neo4jClient.UnboundRunnableSpec querySpec = mock(Neo4jClient.UnboundRunnableSpec.class);
        Neo4jClient.RecordFetchSpec<Map<String, Object>> fetchSpec = mock(Neo4jClient.RecordFetchSpec.class);
        when(querySpec.fetch()).thenReturn(fetchSpec);
        when(fetchSpec.all()).thenReturn(Collections.emptyList());
        when(neo4jClient.query(anyString())).thenReturn(querySpec);

        mockMvc.perform(get("/api/v1/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deepseek.apiKey").doesNotExist())
                .andExpect(jsonPath("$.deepseek.apiKeyConfigured").exists());
    }

    /** 测试专用子类：用固定值覆盖配置，避免依赖反射 */
    static class TestSystemController extends SystemController {
        TestSystemController(Neo4jClient neo4jClient,
                             CourseRepository courseRepository,
                             ResourceFileRepository resourceFileRepository,
                             KnowledgeNodeRepository knowledgeNodeRepository) {
            super(neo4jClient, courseRepository, resourceFileRepository, knowledgeNodeRepository);
        }

        @Override
        protected String deepSeekModel() {
            return "deepseek-v4-pro";
        }

        @Override
        protected String deepSeekApiKey() {
            return "sk-test-key";
        }

        @Override
        protected String neo4jUri() {
            return "neo4j+s://test.databases.neo4j.io";
        }
    }
}
