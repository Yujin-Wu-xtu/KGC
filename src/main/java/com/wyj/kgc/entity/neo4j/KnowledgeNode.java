package com.wyj.kgc.entity.neo4j;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识图谱节点实体
 * Represents a node in the Neo4j graph.
 */
@Node("KnowledgeNode") // 在 Neo4j 中，默认 Label 为 KnowledgeNode
@Data
@NoArgsConstructor
public class KnowledgeNode {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 节点名称 (e.g., "Java编程思想")
     */
    private String name;

    /**
     * 节点标签/类型 (e.g., "Book", "Concept", "Person")
     * 注意：虽然 Neo4j 支持多标签，这里为了简化，先使用单标签字段
     */
    private String label;

    /**
     * 溯源 ID
     * 对应 MySQL 中 resource_file 表的主键 ID
     * 用于追踪这个知识点是从哪个文件里提取出来的
     */
    private Long sourceFileId;

    /**
     * 动态属性
     * 用于存储 AI 提取出来的其他非结构化属性
     */
    // 注意：SDN 对 Map 属性的支持需要配置，或者使用 @CompositeProperty
    // 这里先作为普通属性存储，实际开发可能需要自定义 Converter
    // private Map<String, Object> properties = new HashMap<>();

    public KnowledgeNode(String name, String label, Long sourceFileId) {
        this.name = name;
        this.label = label;
        this.sourceFileId = sourceFileId;
    }
}
