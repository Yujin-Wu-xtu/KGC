package com.wyj.kgc;

import com.wyj.kgc.controller.GraphController;
import com.wyj.kgc.dto.graph.GraphDataDTO;
import com.wyj.kgc.dto.graph.LinkDTO;
import com.wyj.kgc.dto.graph.NodeDTO;
import com.wyj.kgc.service.kg.KnowledgeGraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GraphSaveControllerTest {

    private KnowledgeGraphService knowledgeGraphService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        knowledgeGraphService = mock(KnowledgeGraphService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GraphController(knowledgeGraphService)).build();
    }

    @Test
    void saveCourseGraphBindsCourseAndReturnsNodeCount() throws Exception {
        when(knowledgeGraphService.saveCourseGraph(1L)).thenReturn(Map.of(
                "courseId", 1L,
                "courseName", "数据结构",
                "graphSaved", true,
                "nodeCount", 12));

        mockMvc.perform(post("/api/v1/courses/1/graph/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(1))
                .andExpect(jsonPath("$.courseName").value("数据结构"))
                .andExpect(jsonPath("$.graphSaved").value(true))
                .andExpect(jsonPath("$.nodeCount").value(12));
    }

    @Test
    void saveCourseGraphReportsNullNodeCountWhenGraphDbUnavailable() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("courseId", 2L);
        result.put("courseName", "计算机组成原理");
        result.put("graphSaved", true);
        result.put("nodeCount", null);
        when(knowledgeGraphService.saveCourseGraph(2L)).thenReturn(result);

        mockMvc.perform(post("/api/v1/courses/2/graph/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graphSaved").value(true))
                .andExpect(jsonPath("$.nodeCount").doesNotExist());
    }

    @Test
    void saveCourseGraphReturnsBadRequestWhenCourseMissing() throws Exception {
        when(knowledgeGraphService.saveCourseGraph(999L))
                .thenThrow(new RuntimeException("Course does not exist."));

        mockMvc.perform(post("/api/v1/courses/999/graph/save"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Course does not exist."));
    }

    @Test
    void getGraphByCourseReturnsGraphData() throws Exception {
        NodeDTO node = new NodeDTO("11", "数组", "Concept", 5L);
        LinkDTO link = new LinkDTO("21", "11", "12", "CONTAINS");
        when(knowledgeGraphService.getGraphDataByCourse(1L))
                .thenReturn(new GraphDataDTO(List.of(node), List.of(link)));

        mockMvc.perform(get("/api/v1/courses/1/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].id").value("11"))
                .andExpect(jsonPath("$.nodes[0].name").value("数组"))
                .andExpect(jsonPath("$.links[0].type").value("CONTAINS"));
    }

    @Test
    void getGraphByCourseAcceptsJsonContentType() throws Exception {
        // 前端 fetch 默认可能带 Accept: */*，这里验证不因 Content-Type 而失败
        mockMvc.perform(get("/api/v1/courses/1/graph")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
