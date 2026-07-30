package com.wyj.kgc.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDetailDTO {
    private String id;
    private String name;
    private String label;
    private Long sourceFileId;
    private Long courseId;

    /** 与该节点有关联关系的节点列表 */
    private List<RelatedNode> relatedNodes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedNode {
        private String id;
        private String name;
        private String label;
        private String relationType;
        /** 关系的方向: "out" (当前节点指向它) 或 "in" (它指向当前节点) */
        private String direction;
    }
}
