package com.wyj.kgc.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建或更新知识关系的请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipRequest {
    /** 起点节点的数据库 ID */
    private Long sourceNodeId;
    /** 终点节点的数据库 ID */
    private Long targetNodeId;
    /** 关系类型 (e.g., "CONTAINS", "RELATED_TO", "PREREQUISITE") */
    private String type;
}
