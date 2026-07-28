package com.wyj.kgc.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDTO {
    private String id;
    private String name;
    private String label;
    private Long sourceFileId;

    /** 保留旧的3参数构造函数做兼容 */
    public NodeDTO(String id, String name, String label) {
        this.id = id;
        this.name = name;
        this.label = label;
    }
}
