package com.wyj.kgc.entity.neo4j;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

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
     * 课程 ID
     * 对应 MySQL 中 courses 表的主键 ID
     * 用于将节点隔离到特定课程，防止跨课程的同名节点被误合并
     */
    private Long courseId;

    public KnowledgeNode(String name, String label, Long sourceFileId, Long courseId) {
        this.name = name;
        this.label = label;
        this.sourceFileId = sourceFileId;
        this.courseId = courseId;
    }
}
