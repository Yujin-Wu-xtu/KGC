package com.wyj.kgc.entity.neo4j;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识图谱关系实体 (DTO/POJO)
 * 
 * 注意：在 Spring Data Neo4j 中，处理动态类型的关系比较复杂。
 * 为了方便 AI 提取和数据传输，我们先把它定义为一个独立的 POJO。
 * 
 * 在实际存储时，我们可能会通过自定义 Cypher 语句或特定的 @RelationshipProperties 来实现。
 */
@Data
@NoArgsConstructor
public class KnowledgeRelation {

    private Long id;

    /**
     * 关系类型 (e.g., "IS_A", "CONTAINS", "PREREQUISITE")
     */
    private String type;

    /**
     * 关系的起点名称或 ID
     */
    private String startNodeName;

    /**
     * 关系的终点名称或 ID
     */
    private String endNodeName;

    public KnowledgeRelation(String startNodeName, String type, String endNodeName) {
        this.startNodeName = startNodeName;
        this.type = type;
        this.endNodeName = endNodeName;
    }
}
