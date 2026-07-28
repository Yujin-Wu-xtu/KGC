package com.wyj.kgc.controller;

import com.wyj.kgc.dto.graph.GraphDataDTO;
import com.wyj.kgc.service.kg.KnowledgeGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*") // 允许任何情况下的跨域请求，方便测试
@RestController
@RequestMapping("/api/v1")
public class GraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    @Autowired
    public GraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * Get the full knowledge graph (backward-compatible).
     * GET /api/v1/graph/all
     */
    @GetMapping("/graph/all")
    public ResponseEntity<GraphDataDTO> getAllGraphData() {
        GraphDataDTO graphData = knowledgeGraphService.getFullGraphData();
        return ResponseEntity.ok(graphData);
    }

    /**
     * 按课程 ID 查询该课程的知识图谱。
     * GET /api/v1/courses/{courseId}/graph
     */
    @GetMapping("/courses/{courseId}/graph")
    public ResponseEntity<GraphDataDTO> getGraphByCourse(@PathVariable Long courseId) {
        GraphDataDTO graphData = knowledgeGraphService.getGraphDataByCourse(courseId);
        return ResponseEntity.ok(graphData);
    }
}
