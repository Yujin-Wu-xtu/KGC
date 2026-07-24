package com.wyj.kgc.dto;

import com.wyj.kgc.entity.neo4j.KnowledgeNode;
import com.wyj.kgc.entity.neo4j.KnowledgeRelation;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 图数据传输对象
 * 用于封装从 AI 服务提取出来的一整套节点和关系
 */
@Data
public class GraphData {
    private List<KnowledgeNode> nodes = new ArrayList<>();
    private List<KnowledgeRelation> relations = new ArrayList<>();

    public void addNode(KnowledgeNode node) {
        this.nodes.add(node);
    }

    public void addRelation(KnowledgeRelation relation) {
        this.relations.add(relation);
    }
}
