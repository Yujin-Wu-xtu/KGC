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
                    KnowledgeNode node = new KnowledgeNode(name, "Concept", fileId);
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
                    KnowledgeNode existing = knowledgeNodeRepository.findByName(node.getName());
                    if (existing != null) {
                        node.setId(existing.getId());
                    }
                    knowledgeNodeRepository.save(node);
                }
                System.out.println("DEBUG: Nodes saved to Neo4j.");

                for (KnowledgeRelation relation : relations) {
                    String cypher = "MATCH (head:KnowledgeNode {name: $headName}), (tail:KnowledgeNode {name: $tailName}) "
                            + "MERGE (head)-[:" + relation.getType() + "]->(tail)";
                    neo4jClient.query(cypher)
                            .bind(relation.getStartNodeName()).to("headName")
                            .bind(relation.getEndNodeName()).to("tailName")
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
     * Retrieves the entire knowledge graph for visualization.
     * 
     * @return DTO containing all nodes and relationships.
     */
    public com.wyj.kgc.dto.graph.GraphDataDTO getFullGraphData() {
        // Query all nodes
        List<KnowledgeNode> nodes = knowledgeNodeRepository.findAll();
        List<com.wyj.kgc.dto.graph.NodeDTO> nodeDTOs = new ArrayList<>();
        for (KnowledgeNode node : nodes) {
            // Using ID as string for compatibility with various frontend libs
            nodeDTOs.add(new com.wyj.kgc.dto.graph.NodeDTO(
                    String.valueOf(node.getId()),
                    node.getName(),
                    node.getLabel()));
        }

        // Query all relationships using raw Cypher
        // We match any relationship [r] between any two nodes (n, m)
        String cypher = "MATCH (n)-[r]->(m) RETURN n.name as source, m.name as target, type(r) as type";

        List<com.wyj.kgc.dto.graph.LinkDTO> linkDTOs = new ArrayList<>();

        neo4jClient.query(cypher)
                .fetch()
                .all()
                .forEach(record -> {
                    String source = (String) record.get("source");
                    String target = (String) record.get("target");
                    String type = (String) record.get("type");

                    // For visualization, we often need IDs, but if names are unique enough or
                    // mapped, we can use names
                    // However, to be robust with the NodeDTOs which use ID, we should probably
                    // output IDs from the Cypher query
                    // Let's stick to names for source/target in LinkDTO for now as requested by
                    // user ("source（起点节点 ID 或名称）")
                    // If frontend needs IDs, we can adjust the Cypher to return IDs.
                    linkDTOs.add(new com.wyj.kgc.dto.graph.LinkDTO(source, target, type));
                });

        return new com.wyj.kgc.dto.graph.GraphDataDTO(nodeDTOs, linkDTOs);
    }
}
