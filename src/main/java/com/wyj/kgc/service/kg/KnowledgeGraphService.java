package com.wyj.kgc.service.kg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyj.kgc.entity.ResourceFile;
import com.wyj.kgc.entity.neo4j.KnowledgeNode;
import com.wyj.kgc.entity.neo4j.KnowledgeRelation;
import com.wyj.kgc.repository.jpa.ResourceFileRepository;
import com.wyj.kgc.repository.neo4j.KnowledgeNodeRepository;
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

    @Autowired
    public KnowledgeGraphService(ResourceFileRepository resourceFileRepository,
            KnowledgeNodeRepository knowledgeNodeRepository,
            Neo4jClient neo4jClient,
            DeepSeekClient deepSeekClient,
            PdfUtils pdfUtils,
            WordUtils wordUtils,
            PptUtils pptUtils,
            ObjectMapper objectMapper) {
        this.resourceFileRepository = resourceFileRepository;
        this.knowledgeNodeRepository = knowledgeNodeRepository;
        this.neo4jClient = neo4jClient;
        this.deepSeekClient = deepSeekClient;
        this.pdfUtils = pdfUtils;
        this.wordUtils = wordUtils;
        this.pptUtils = pptUtils;
        this.objectMapper = objectMapper;
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

        // 构建关系查询 Cypher：统一使用 id(n) 和 id(m) 作为 source/target
        String cypher;
        if (courseId != null) {
            cypher = "MATCH (n:KnowledgeNode {courseId: $courseId})-[r]->(m:KnowledgeNode {courseId: $courseId}) "
                   + "RETURN id(n) as sourceId, id(m) as targetId, type(r) as type";
        } else {
            cypher = "MATCH (n)-[r]->(m) RETURN id(n) as sourceId, id(m) as targetId, type(r) as type";
        }

        List<com.wyj.kgc.dto.graph.LinkDTO> linkDTOs = new ArrayList<>();

        if (courseId != null) {
            neo4jClient.query(cypher)
                    .bind(courseId).to("courseId")
                    .fetch().all().forEach(record -> {
                        String source = String.valueOf(record.get("sourceId"));
                        String target = String.valueOf(record.get("targetId"));
                        String type = (String) record.get("type");
                        linkDTOs.add(new com.wyj.kgc.dto.graph.LinkDTO(source, target, type));
                    });
        } else {
            neo4jClient.query(cypher)
                    .fetch().all().forEach(record -> {
                        String source = String.valueOf(record.get("sourceId"));
                        String target = String.valueOf(record.get("targetId"));
                        String type = (String) record.get("type");
                        linkDTOs.add(new com.wyj.kgc.dto.graph.LinkDTO(source, target, type));
                    });
        }

        return new com.wyj.kgc.dto.graph.GraphDataDTO(nodeDTOs, linkDTOs);
    }
}
