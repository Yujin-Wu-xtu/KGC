package com.wyj.kgc.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkDTO {
    private String id;
    private String source;
    private String target;
    private String type;

    /** 保留旧的3参数构造函数做兼容 */
    public LinkDTO(String source, String target, String type) {
        this.source = source;
        this.target = target;
        this.type = type;
    }
}
