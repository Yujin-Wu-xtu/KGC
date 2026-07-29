package com.wyj.kgc.controller;

import com.wyj.kgc.dto.graph.GraphDataDTO;
import com.wyj.kgc.dto.graph.NodeRequest;
import com.wyj.kgc.dto.graph.RelationshipRequest;
import com.wyj.kgc.entity.neo4j.KnowledgeNode;
import com.wyj.kgc.service.kg.KnowledgeGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1")
public class GraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    @Autowired
    public GraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    // ===================== 图谱查询 =====================

    /** GET /api/v1/graph/all — 全局图谱（向后兼容） */
    @GetMapping("/graph/all")
    public ResponseEntity<GraphDataDTO> getAllGraphData() {
        return ResponseEntity.ok(knowledgeGraphService.getFullGraphData());
    }

    /** GET /api/v1/courses/{courseId}/graph — 按课程查询图谱 */
    @GetMapping("/courses/{courseId}/graph")
    public ResponseEntity<GraphDataDTO> getGraphByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(knowledgeGraphService.getGraphDataByCourse(courseId));
    }

    // ===================== 节点 CRUD =====================

    /** POST /api/v1/courses/{courseId}/graph/nodes — 添加节点 */
    @PostMapping("/courses/{courseId}/graph/nodes")
    public ResponseEntity<?> addNode(@PathVariable Long courseId, @RequestBody NodeRequest request) {
        try {
            KnowledgeNode node = knowledgeGraphService.addNode(courseId, request.getName(), request.getLabel());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", node.getId(),
                    "name", node.getName(),
                    "label", node.getLabel()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** PUT /api/v1/courses/{courseId}/graph/nodes/{nodeId} — 更新节点 */
    @PutMapping("/courses/{courseId}/graph/nodes/{nodeId}")
    public ResponseEntity<?> updateNode(@PathVariable Long courseId, @PathVariable Long nodeId,
                                        @RequestBody NodeRequest request) {
        try {
            KnowledgeNode node = knowledgeGraphService.updateNode(courseId, nodeId, request.getName(), request.getLabel());
            return ResponseEntity.ok(Map.of(
                    "id", node.getId(),
                    "name", node.getName(),
                    "label", node.getLabel()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** DELETE /api/v1/courses/{courseId}/graph/nodes/{nodeId} — 删除节点（连带关系） */
    @DeleteMapping("/courses/{courseId}/graph/nodes/{nodeId}")
    public ResponseEntity<?> deleteNode(@PathVariable Long courseId, @PathVariable Long nodeId) {
        try {
            knowledgeGraphService.deleteNode(courseId, nodeId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ===================== 关系 CRUD =====================

    /** POST /api/v1/courses/{courseId}/graph/relationships — 添加关系 */
    @PostMapping("/courses/{courseId}/graph/relationships")
    public ResponseEntity<?> addRelationship(@PathVariable Long courseId, @RequestBody RelationshipRequest request) {
        try {
            knowledgeGraphService.addRelationship(courseId, request.getSourceNodeId(), request.getTargetNodeId(), request.getType());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "关系创建成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** PUT /api/v1/courses/{courseId}/graph/relationships/{relId} — 更新关系类型 */
    @PutMapping("/courses/{courseId}/graph/relationships/{relId}")
    public ResponseEntity<?> updateRelationship(@PathVariable Long courseId, @PathVariable Long relId,
                                                @RequestBody RelationshipRequest request) {
        try {
            knowledgeGraphService.updateRelationship(courseId, relId, request.getType());
            return ResponseEntity.ok(Map.of("message", "关系更新成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** DELETE /api/v1/courses/{courseId}/graph/relationships/{relId} — 删除关系 */
    @DeleteMapping("/courses/{courseId}/graph/relationships/{relId}")
    public ResponseEntity<?> deleteRelationship(@PathVariable Long courseId, @PathVariable Long relId) {
        try {
            knowledgeGraphService.deleteRelationship(courseId, relId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

