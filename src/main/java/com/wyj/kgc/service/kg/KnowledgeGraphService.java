package com.wyj.kgc.service.kg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyj.kgc.entity.ResourceFile;
import com.wyj.kgc.entity.Course;
import com.wyj.kgc.entity.neo4j.KnowledgeNode;
import com.wyj.kgc.entity.neo4j.KnowledgeRelation;
import com.wyj.kgc.repository.jpa.ResourceFileRepository;
import com.wyj.kgc.repository.neo4j.KnowledgeNodeRepository;
import com.wyj.kgc.service.CourseService;
import com.wyj.kgc.service.DeepSeekClient;
import com.wyj.kgc.utils.PdfUtils;
import com.wyj.kgc.utils.WordUtils;
import com.wyj.kgc.utils.PptUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeGraphService {

    private final ResourceFileRepository resourceFileRepository;
    private final KnowledgeNodeRepository knowledgeNodeRepository;
    private final Neo4jClient neo4jClient;
    private final DeepSeekClient deepSeekClient;
    private final PdfUtils pdfUtils;
    private final WordUtils wordUtils;
    private final PptUtils pptUtils;
    private final ObjectMapper objectMapper;
    private final CourseService courseService;

    @Autowired
    public KnowledgeGraphService(ResourceFileRepository resourceFileRepository,
            KnowledgeNodeRepository knowledgeNodeRepository,
            Neo4jClient neo4jClient,
            DeepSeekClient deepSeekClient,
            PdfUtils pdfUtils,
            WordUtils wordUtils,
            PptUtils pptUtils,
            ObjectMapper objectMapper,
            CourseService courseService) {
        this.resourceFileRepository = resourceFileRepository;
        this.knowledgeNodeRepository = knowledgeNodeRepository;
        this.neo4jClient = neo4jClient;
        this.deepSeekClient = deepSeekClient;
        this.pdfUtils = pdfUtils;
        this.wordUtils = wordUtils;
        this.pptUtils = pptUtils;
        this.objectMapper = objectMapper;
        this.courseService = courseService;
    }

    public String parseAndSave(Long fileId) {
        System.out.println("DEBUG: parseAndSave called for fileId: " + fileId);

        try {
            // 1. Get Resource File
            ResourceFile resourceFile = resourceFileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

            // 获取课程 ID，用于课程级隔离
            Long courseId = resourceFile.getCourseId();
            if (courseId == null) {
                throw new RuntimeException("File is not associated with a course: " + fileId);
            }

            // 2. Extract Text from PDF or Word
            File file = new File(resourceFile.getFilePath());
            if (!file.exists()) {
                throw new RuntimeException("Physical file not found at: " + resourceFile.getFilePath());
            }
            
            String extractedText;
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".pdf")) {
                extractedText = pdfUtils.extractText(file);
            } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
                extractedText = wordUtils.extractText(file);
            } else if (fileName.endsWith(".ppt") || fileName.endsWith(".pptx")) {
                extractedText = pptUtils.extractText(file);
            } else {
                throw new RuntimeException("Unsupported file format: " + fileName);
            }
            
            System.out.println("DEBUG: Extracted text length: " + extractedText.length());

            // 3. Call AI to Extract Knowledge
            String jsonOutput = deepSeekClient.extractKnowledge(extractedText);
            System.out.println("DEBUG: AI Response: " + jsonOutput);

            // 4. Parse JSON Response
            JsonNode rootNode = objectMapper.readTree(jsonOutput);
            JsonNode nodesArray = rootNode.get("nodes");
            JsonNode relationshipsArray = rootNode.get("relationships");

            List<KnowledgeNode> nodes = new ArrayList<>();
            List<KnowledgeRelation> relations = new ArrayList<>();

            if (nodesArray != null && nodesArray.isArray()) {
                for (JsonNode nodeJson : nodesArray) {
                    String name = nodeJson.has("name") ? nodeJson.get("name").asText() : "Unknown";
                    KnowledgeNode node = new KnowledgeNode(name, "Concept", fileId, courseId);
                    nodes.add(node);
                }
            }

            if (relationshipsArray != null && relationshipsArray.isArray()) {
                for (JsonNode relJson : relationshipsArray) {
                    String from = relJson.has("from") ? relJson.get("from").asText() : "";
                    String to = relJson.has("to") ? relJson.get("to").asText() : "";
                    String type = relJson.has("type") ? relJson.get("type").asText() : "RELATED_TO";

                    if (!from.isEmpty() && !to.isEmpty()) {
                        relations.add(new KnowledgeRelation(from, type, to));
                    }
                }
            }

            // 5. 尝试持久化到 Neo4j（失败不阻断流程）
            try {
                for (KnowledgeNode node : nodes) {
                    // 课程内去重：同一课程内同名节点不重复创建
                    KnowledgeNode existing = knowledgeNodeRepository.findByNameAndCourseId(node.getName(), courseId);
                    if (existing != null) {
                        node.setId(existing.getId());
                    }
                    knowledgeNodeRepository.save(node);
                }
                System.out.println("DEBUG: Nodes saved to Neo4j with courseId=" + courseId);

                for (KnowledgeRelation relation : relations) {
                    // 关系查询加上 courseId 约束，防止跨课程连线
                    String cypher = "MATCH (head:KnowledgeNode {name: $headName, courseId: $courseId}), "
                            + "(tail:KnowledgeNode {name: $tailName, courseId: $courseId}) "
                            + "MERGE (head)-[:" + relation.getType() + "]->(tail)";
                    neo4jClient.query(cypher)
                            .bind(relation.getStartNodeName()).to("headName")
                            .bind(relation.getEndNodeName()).to("tailName")
                            .bind(courseId).to("courseId")
                            .run();
                }
                System.out.println("DEBUG: Relations saved to Neo4j.");
            } catch (Exception neo4jEx) {
                System.out.println("WARN: Neo4j 持久化失败（不影响返回结果）: " + neo4jEx.getMessage());
            }

            // 6. 无论 Neo4j 是否成功，都返回 DeepSeek 提取的原始 JSON
            return jsonOutput;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse and save knowledge graph: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the entire knowledge graph for visualization (backward-compatible).
     * 
     * @return DTO containing all nodes and relationships.
     */
    public com.wyj.kgc.dto.graph.GraphDataDTO getFullGraphData() {
        List<KnowledgeNode> nodes = knowledgeNodeRepository.findAll();
        return buildGraphDataDTO(nodes, null);
    }

    /**
     * 按课程 ID 查询该课程的知识图谱。
     * 节点和关系都限定在该课程范围内，不会跨课程。
     *
     * @param courseId 课程 ID
     * @return 该课程的图谱数据
     */
    public com.wyj.kgc.dto.graph.GraphDataDTO getGraphDataByCourse(Long courseId) {
        List<KnowledgeNode> nodes = knowledgeNodeRepository.findByCourseId(courseId);
        return buildGraphDataDTO(nodes, courseId);
    }

    /**
     * 保存课程图谱：将当前课程的图谱与课程绑定。
     * 1. 校验课程存在。
     * 2. 统计该课程在 Neo4j 中的节点数（图数据库不可用时返回 null，不阻断保存）。
     * 3. 将课程 graphSaved 置为 true。
     *
     * @param courseId 课程 ID
     * @return 保存结果：课程信息 + 节点数（可能为 null 表示图数据库暂不可用）
     */
    public java.util.Map<String, Object> saveCourseGraph(Long courseId) {
        Course course = courseService.setCourseGraphSaved(courseId, true);

        // 统计节点数；Neo4j 异常时给出 null，不阻断绑定流程
        Integer nodeCount = null;
        try {
            nodeCount = knowledgeNodeRepository.findByCourseId(courseId).size();
        } catch (Exception neo4jEx) {
            System.out.println("WARN: Neo4j 节点统计失败（不影响保存）: " + neo4jEx.getMessage());
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("courseId", course.getId());
        result.put("courseName", course.getName());
        result.put("graphSaved", true);
        result.put("nodeCount", nodeCount);
        return result;
    }

    /**
     * 内部方法：从节点列表构建 GraphDataDTO。
     * 当 courseId 不为 null 时，关系查询限定在该课程内。
     */
    private com.wyj.kgc.dto.graph.GraphDataDTO buildGraphDataDTO(List<KnowledgeNode> nodes, Long courseId) {
        List<com.wyj.kgc.dto.graph.NodeDTO> nodeDTOs = new ArrayList<>();
        for (KnowledgeNode node : nodes) {
            nodeDTOs.add(new com.wyj.kgc.dto.graph.NodeDTO(
                    String.valueOf(node.getId()),
                    node.getName(),
                    node.getLabel(),
                    node.getSourceFileId()));
        }

        // Cypher 查询返回关系 ID、起点 ID、终点 ID 和关系类型
        String cypher;
        if (courseId != null) {
            cypher = "MATCH (n:KnowledgeNode {courseId: $courseId})-[r]->(m:KnowledgeNode {courseId: $courseId}) "
                   + "RETURN id(r) as relId, id(n) as sourceId, id(m) as targetId, type(r) as type";
        } else {
            cypher = "MATCH (n)-[r]->(m) RETURN id(r) as relId, id(n) as sourceId, id(m) as targetId, type(r) as type";
        }

        List<com.wyj.kgc.dto.graph.LinkDTO> linkDTOs = new ArrayList<>();

        if (courseId != null) {
            neo4jClient.query(cypher)
                    .bind(courseId).to("courseId")
                    .fetch().all().forEach(record -> {
                        linkDTOs.add(new com.wyj.kgc.dto.graph.LinkDTO(
                                String.valueOf(record.get("relId")),
                                String.valueOf(record.get("sourceId")),
                                String.valueOf(record.get("targetId")),
                                (String) record.get("type")));
                    });
        } else {
            neo4jClient.query(cypher)
                    .fetch().all().forEach(record -> {
                        linkDTOs.add(new com.wyj.kgc.dto.graph.LinkDTO(
                                String.valueOf(record.get("relId")),
                                String.valueOf(record.get("sourceId")),
                                String.valueOf(record.get("targetId")),
                                (String) record.get("type")));
                    });
        }

        return new com.wyj.kgc.dto.graph.GraphDataDTO(nodeDTOs, linkDTOs);
    }

    // ===================== 节点 CRUD =====================

    /**
     * 在指定课程中添加一个新节点。
     */
    public KnowledgeNode addNode(Long courseId, String name, String label) {
        // 课程内同名检查
        KnowledgeNode existing = knowledgeNodeRepository.findByNameAndCourseId(name, courseId);
        if (existing != null) {
            throw new RuntimeException("该课程中已存在同名节点: " + name);
        }
        KnowledgeNode node = new KnowledgeNode(name, label != null ? label : "Concept", null, courseId);
        return knowledgeNodeRepository.save(node);
    }

    /**
     * 更新节点名称和/或标签。
     */
    public KnowledgeNode updateNode(Long courseId, Long nodeId, String name, String label) {
        KnowledgeNode node = knowledgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
        if (!courseId.equals(node.getCourseId())) {
            throw new RuntimeException("该节点不属于此课程");
        }
        // 如果改名了，检查新名称在课程内是否冲突
        if (name != null && !name.equals(node.getName())) {
            KnowledgeNode conflict = knowledgeNodeRepository.findByNameAndCourseId(name, courseId);
            if (conflict != null && !conflict.getId().equals(nodeId)) {
                throw new RuntimeException("该课程中已存在同名节点: " + name);
            }
            node.setName(name);
        }
        if (label != null) {
            node.setLabel(label);
        }
        return knowledgeNodeRepository.save(node);
    }

    /**
     * 删除节点及其所有关系。
     */
    public void deleteNode(Long courseId, Long nodeId) {
        KnowledgeNode node = knowledgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));
        if (!courseId.equals(node.getCourseId())) {
            throw new RuntimeException("该节点不属于此课程");
        }
        // DETACH DELETE 会同时删除节点的所有关系
        neo4jClient.query("MATCH (n) WHERE id(n) = $nodeId DETACH DELETE n")
                .bind(nodeId).to("nodeId")
                .run();
    }

    // ===================== 关系 CRUD =====================

    // ===================== 节点查询（学生端） =====================

    /**
     * 获取单个节点的详细信息，包括其关联节点。
     * 用于学生学习页面的沉浸式视图。
     */
    public com.wyj.kgc.dto.graph.NodeDetailDTO getNodeDetail(Long nodeId) {
        KnowledgeNode node = knowledgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));

        // 查询关联节点：MATCH (n)-[r]-(m) WHERE id(n) = $nodeId
        String cypher = "MATCH (n:KnowledgeNode)-[r]-(m:KnowledgeNode) WHERE id(n) = $nodeId "
                + "RETURN id(m) as relatedId, m.name as relatedName, m.label as relatedLabel, "
                + "type(r) as relationType, "
                + "CASE WHEN startNode(r) = n THEN 'out' ELSE 'in' END as direction";

        List<com.wyj.kgc.dto.graph.NodeDetailDTO.RelatedNode> relatedNodes = new ArrayList<>();
        neo4jClient.query(cypher)
                .bind(nodeId).to("nodeId")
                .fetch().all().forEach(record -> {
                    relatedNodes.add(new com.wyj.kgc.dto.graph.NodeDetailDTO.RelatedNode(
                            String.valueOf(record.get("relatedId")),
                            (String) record.get("relatedName"),
                            (String) record.get("relatedLabel"),
                            (String) record.get("relationType"),
                            (String) record.get("direction")));
                });

        return new com.wyj.kgc.dto.graph.NodeDetailDTO(
                String.valueOf(node.getId()),
                node.getName(),
                node.getLabel(),
                node.getSourceFileId(),
                node.getCourseId(),
                relatedNodes);
    }

    // ===================== 关系 CRUD =====================

    /**
     * 在指定课程中添加一条关系。
     * 起点和终点必须都属于该课程。
     */
    public void addRelationship(Long courseId, Long sourceNodeId, Long targetNodeId, String type) {
        if (type == null || type.isBlank()) {
            type = "RELATED_TO";
        }
        // 验证类型名称合法性（Neo4j 关系类型只允许字母、数字和下划线）
        if (!type.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
            throw new RuntimeException("关系类型名称不合法（仅允许字母、数字和下划线）: " + type);
        }
        // 确保两个节点都存在且属于该课程
        String cypher = "MATCH (a:KnowledgeNode), (b:KnowledgeNode) "
                + "WHERE id(a) = $sourceId AND id(b) = $targetId "
                + "AND a.courseId = $courseId AND b.courseId = $courseId "
                + "MERGE (a)-[:" + type + "]->(b)";
        neo4jClient.query(cypher)
                .bind(sourceNodeId).to("sourceId")
                .bind(targetNodeId).to("targetId")
                .bind(courseId).to("courseId")
                .run();
    }

    /**
     * 更新关系类型（删除旧关系并创建同方向的新类型关系）。
     */
    public void updateRelationship(Long courseId, Long relationshipId, String newType) {
        if (newType == null || newType.isBlank()) {
            throw new RuntimeException("新的关系类型不能为空");
        }
        if (!newType.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
            throw new RuntimeException("关系类型名称不合法: " + newType);
        }
        // Neo4j 不能直接修改关系类型，需要先拿到起终点，删旧建新
        String cypher = "MATCH (a)-[r]->(b) WHERE id(r) = $relId "
                + "AND a.courseId = $courseId AND b.courseId = $courseId "
                + "DELETE r "
                + "WITH a, b "
                + "MERGE (a)-[:" + newType + "]->(b)";
        neo4jClient.query(cypher)
                .bind(relationshipId).to("relId")
                .bind(courseId).to("courseId")
                .run();
    }

    /**
     * 删除一条关系。
     */
    public void deleteRelationship(Long courseId, Long relationshipId) {
        String cypher = "MATCH (a)-[r]->(b) WHERE id(r) = $relId "
                + "AND a.courseId = $courseId "
                + "DELETE r";
        neo4jClient.query(cypher)
                .bind(relationshipId).to("relId")
                .bind(courseId).to("courseId")
                .run();
    }
}

