# PROGRESS: Knowledge Graph Backend (Dual DB Integration) - 2026-02-19

## 1. 架构升级：双数据库事务共存 (MySQL + Neo4j)
- **Status**: ✅ Completed
- **Details**:
  - 成功接入 **Neo4j Aura 云端数据库**，实现了与现有 **MySQL (Spring Data JPA)** 的双数据源共存。
  - **解决核心痛点：事务冲突 (TransactionManager Conflict)**
    - *问题*：Spring Boot 默认的 `Neo4jTransactionManager` 与 `JpaTransactionManager` 发生冲突，导致 `@Transactional` 注解可能“吞掉”Neo4j的提交操作，或者引发 500 错误。
    - *解决方案*：在 `KnowledgeGraphService` 中移除了 `@Transactional("transactionManager")` 注解，采用**非事务性/独立事务**的方式调用 Neo4j Repository，确保 `save()` 操作能立即真实落库，不再被 JPA 事务管理器回滚或忽略。

## 2. 权限配置：解析接口白名单
- **Status**: ✅ Completed
- **Details**:
  - 修改了 `SecurityConfig.java`，针对图谱解析相关的 API 路径实施了免登录策略。
  - **关键配置**: `.requestMatchers("/api/v1/files/**").permitAll()`
  - **目的**: 在开发调试阶段，允许通过 `curl` 或 Postman 直接触发 `/parse-graph` 接口，无需每次重新根据 Token 进行鉴权，极大提升了从上传到解析的联调效率。

## 3. 核心功能闭环：Mock 数据全流程落库
- **Status**: ✅ Verified (End-to-End)
- **Overview**: 成功打通了从“文件上传”到“图谱落库”的完整链路。
- **Implementation**:
  1.  **上传 (MySQL)**: 文件元数据成功写入 `resource_files` 表。
  2.  **解析 (Mock)**: 
      - 针对文件名乱码和文件读取问题，我们在 `KnowledgeExtractionServiceImpl` 中实施了**强编码 (Hardcoding)** 策略。
      - 绕过真实文件读取，强制返回预设的知识结构。
  3.  **落库 (Neo4j)**:
      - **节点**: 成功创建 1 个根节点 `(Course: "数据结构")` 和 2 个子节点 `(Concept: "数组")`, `(Concept: "链表")`。
      - **关系**: 成功建立 `(数据结构)-[:CONTAINS]->(数组)` 和 `(数据结构)-[:CONTAINS]->(链表)` 的语义关系。

## 4. AI 接入里程碑：DeepSeek API 连通性测试 (DeepSeek API Connectivity)
- **Status**: ✅ Verified
- **Details**:
  - 成功编写并通过了 `DeepSeekPingTest` 测试类。
  - **连通性验证**: 能够成功调用 DeepSeek API 并获取预期响应。
  - **模型选型**: 确认使用 `deepseek-chat` 模型作为后续图谱构建的核心 LLM。
  - **下一步**: 将测试通过的代码逻辑移植到 `KnowledgeExtractionServiceImpl` 中，替换现有的 Mock 实现。

## 5. 核心功能闭环：真实 AI 解析与图谱可视化接口 (Real AI & Graph API) - 2026-02-20
- **Status**: ✅ Completed & Verified
- **Details**:
  - **全链路闭环**: 成功将真实的 PDF 文件（《贪心排序》）通过 `PdfUtils` 提取纯文本，交由 `DeepSeekClient` 进行实体与关系抽取，并最终落库 Neo4j。
  - **前端接口交付**: 
    - 设计了 `GraphDataDTO`、`NodeDTO` 和 `LinkDTO` 数据结构。
    - 开发了 `GET /api/v1/graph/all` 接口，能够将 Neo4j 中的复杂图结构扁平化转换为 ECharts/G6 等前端可视化图表所需的标准 JSON 格式。
  - **团队协同**: 后端图谱核心功能已就绪，正式进入与前端（Vue）的联调阶段。

## 6. 遗留问题与下一步计划 (Next Steps)
- **当前局限**: 
  - 暂时放行了安全接口方便联调。
  - 大文件解析尚未处理 Token 限制。

- **下一步核心任务**: **前端联调与性能优化**
  1.  **前端联调**: 配合前端完成图谱渲染。
  2.  **长文本突破**: 引入文本分块（Chunking）机制。
## 7. 多模态扩展：PPTX 解析支持 (PPTX Parsing Support) - 2026-05-04
- **Status**: ✅ Completed
- **Details**:
  - **工具类实现**: 新增 `PptUtils.java`，利用 Apache POI 的 `SlideShowExtractor` 实现了对 `.pptx` 和 `.ppt` 文件的统一文本提取。
  - **服务集成**: 在 `KnowledgeGraphService` 中集成了 PPT 解析逻辑。
  - **目标**: 增强平台对多种教学资源格式的兼容性，符合计设大赛“多模态”的技术定位。
