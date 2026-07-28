package com.wyj.kgc.repository.neo4j;

import com.wyj.kgc.entity.neo4j.KnowledgeNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Neo4j 知识节点仓库
 * 负责对 KnowledgeNode 进行增删改查
 */
@Repository
public interface KnowledgeNodeRepository extends Neo4jRepository<KnowledgeNode, Long> {

    /**
     * 根据节点名称查找节点（全局，保留向后兼容）
     */
    KnowledgeNode findByName(String name);

    /**
     * 根据节点名称和课程 ID 查找节点（课程内去重）
     */
    KnowledgeNode findByNameAndCourseId(String name, Long courseId);

    /**
     * 根据课程 ID 查找所有归属于该课程的节点
     */
    List<KnowledgeNode> findByCourseId(Long courseId);

    /**
     * 根据源文件 ID 查找所有归属于该文件的节点 (溯源功能)
     */
    List<KnowledgeNode> findBySourceFileId(Long sourceFileId);
}
