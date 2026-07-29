package com.wyj.kgc.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建或更新知识节点的请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeRequest {
    /** 节点名称 */
    private String name;
    /** 节点标签/类型 (e.g., "Concept", "Property") */
    private String label;
}
