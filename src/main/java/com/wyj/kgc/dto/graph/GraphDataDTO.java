package com.wyj.kgc.dto.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphDataDTO {
    private List<NodeDTO> nodes;
    private List<LinkDTO> links;
}
