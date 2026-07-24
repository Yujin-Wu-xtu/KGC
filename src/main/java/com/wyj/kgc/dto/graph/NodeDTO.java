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
    // Add color or category if needed for ECharts
}
