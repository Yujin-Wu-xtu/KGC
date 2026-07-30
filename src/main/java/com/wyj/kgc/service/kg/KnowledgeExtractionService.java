package com.wyj.kgc.service.kg;

import com.wyj.kgc.dto.GraphData;

/**
 * 知识提取服务接口
 * 定义了如何从文本内容中提取知识图谱的标准契约
 */
public interface KnowledgeExtractionService {

    /**
     * 从给定的文本内容中提取知识图谱数据
     *
     * @param content 需要分析的文本内容 (来自 PDF, Word, TXT 等)
     * @param context 上下文信息 (e.g., "高中数学", "计算机网络", "近代史")
     *                用于构建动态的 System Prompt，告诉 AI 用什么视角去提取
     * @return 包含节点和关系的图数据对象
     */
    GraphData extractKnowledge(String content, String context);
}
