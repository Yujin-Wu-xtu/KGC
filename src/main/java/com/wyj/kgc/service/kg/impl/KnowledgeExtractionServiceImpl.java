package com.wyj.kgc.service.kg.impl;

import com.wyj.kgc.dto.GraphData;
import com.wyj.kgc.entity.neo4j.KnowledgeNode;
import com.wyj.kgc.entity.neo4j.KnowledgeRelation;
import com.wyj.kgc.service.kg.KnowledgeExtractionService;
import org.springframework.stereotype.Service;

/**
 * KnowledgeExtractionService Mock Implementation
 */
@Service
public class KnowledgeExtractionServiceImpl implements KnowledgeExtractionService {

    @Override
    public GraphData extractKnowledge(String content, String context) {
        GraphData graphData = new GraphData();

        // Check if content (or filename passed as content) contains the keyword
        // if (content != null && content.contains("数据结构")) {
        // FORCE EXECUTION:
        {
            System.out.println("DEBUG: Forced Mock Generation (Bypassing filename check)");

            // 1. Create Root Node
            KnowledgeNode root = new KnowledgeNode();
            root.setName("数据结构");
            root.setLabel("Course");
            // sourceFileId will be set by the caller (KnowledgeGraphService)
            graphData.addNode(root);

            // 2. Create Child Nodes
            KnowledgeNode child1 = new KnowledgeNode();
            child1.setName("数组");
            child1.setLabel("Concept");
            graphData.addNode(child1);

            KnowledgeNode child2 = new KnowledgeNode();
            child2.setName("链表");
            child2.setLabel("Concept");
            graphData.addNode(child2);

            // 3. Create Relations
            graphData.addRelation(new KnowledgeRelation("数据结构", "CONTAINS", "数组"));
            graphData.addRelation(new KnowledgeRelation("数据结构", "CONTAINS", "链表"));
        }

        return graphData;
    }
}
