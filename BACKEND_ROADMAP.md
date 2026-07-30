# BACKEND_ROADMAP: Knowledge Graph Construction System

## 1. 架构与技术栈概览 (Architecture & Tech Stack)

本系统采用现代化的 **Spring Boot 3.2.3** 单体架构，核心特点是 **双数据库异构存储**，旨在结合关系型数据库的事务稳定性和图数据库的关系分析能力。

### Core Tech Stack
-   **Language**: Java 17
-   **Framework**: Spring Boot 3.2.3
-   **Security**: Spring Security + JWT (jjwt 0.12.3)
-   **Database (Relational)**: MySQL 8.0 (Stored via Spring Data JPA) - 用于存储用户信息、文件元数据、课程结构。
-   **Database (Graph)**: Neo4j Aura (Stored via Spring Data Neo4j) - 用于存储知识图谱节点 (KnowledgeNode) 和关系 (KnowledgeRelation)。
-   **Tools**: Lombok, Maven

### Architecture Highlights
-   **Dual-DB Transaction Management**: 解决了 JPA 与 Neo4j 事务管理器冲突的问题，实现了 MySQL 强事务与 Neo4j 独立落库的共存模式。
-   **RESTful API**: 遵循标准 REST 规范，提供文件上传、解析触发、图谱查询等接口。

---

## 2. 已完成模块 (Completed Modules - 🟢 100%)

以下模块代码已合并入主分支，并经过完整测试验证（Unit Test / Curl Verification）。

### 🔐 认证与用户中心 (Authentication & User)
-   **核心类**: `AuthController`, `UserService`, `CustomUserDetailsService`, `JwtTokenProvider`
-   **功能**:
    -   用户注册 (`/api/v1/auth/register`)
    -   用户登录与 JWT 签发 (`/api/v1/auth/login`)
    -   基于 RBAC 的角色与权限基础 (User/Role entities)

### 📂 文件存储服务 (File Storage)
-   **核心类**: `FileUploadController`, `FileStorageService`, `ResourceFile`
-   **功能**:
    -   多媒体文件上传 (`/api/v1/files/upload`)
    -   文件元数据 (Filename, Type, Size, Uploader) 存入 MySQL。
    -   本地磁盘文件存储管理。

### 🏗️ 基础设施搭建 (Infrastructure)
-   **配置**: `application.properties`, `SecurityConfig`
-   **功能**:
    -   MySQL 与 Neo4j 双数据源连接池配置。
    -   Spring Security 过滤链配置。
    -   全局异常处理基础框架。

---

## 3. 正在进行的核心模块 (In Progress - 🟡 85%)

当前开发重点在于 **知识图谱的提取与落库链路**。

### 🧠 知识图谱构建 (Knowledge Graph Construction)
-   **核心类**: `KnowledgeGraphService`, `DeepSeekClient`, `PdfUtils`
-   **当前进展**:
    -   ✅ **链路打通**: 成功实现了 `上传 PDF -> 提取文本 -> DeepSeek AI 解析 -> 存入 Neo4j` 的全流程。
    -   ✅ **云端落库**: 数据成功写入 Neo4j Aura。
    -   ✅ **真实解析**: 移除了 Mock 逻辑，实现了基于 DeepSeek V3 的真实知识抽取。
    -   ✅ **可视化接口**: 开发了 `GET /api/v1/graph/all`，返回标准化的 DTO 数据供前端渲染。

### 🛡️ 接口安全 (API Security)
-   **当前状态**: 🟡 进行中 (安全与优化)
-   **配置**:
    -   为了方便联调，`/api/v1/files/**` 和 `/api/v1/graph/**` 接口均在 Spring Security 的白名单中放行。
    -   已配置 CORS 允许跨域请求。

---

## 4. 下一步待开发清单 (To-Do List - 🔴 Pending)

### 🎨 前端联调支持 (Frontend Integration)
-   **目标**: 确保前端能完美渲染图谱。
-   **任务**:
    -   [ ] 根据前端需求微调 DTO 字段（如增加颜色、大小等视觉属性）。
    -   [ ] 配合前端调试跨域和数据格式问题。

### 🚀 性能与长文本优化 (Optimization)
-   **目标**: 支持大文件解析。
-   **任务**:
    -   [ ] **长文本突破**: 引入文本分块（Chunking）机制，解决超过大模型 Token 限制的长篇 PDF 解析问题。

### 🔒 安全闭环 (Security Hardening)
-   **目标**: 恢复零信任安全模型。
-   **任务**:
    -   [ ] 移除 `SecurityConfig` 中的 `.permitAll()` 白名单。
    -   [ ] 验证 UserID 链路，确保用户只能操作自己上传的文件。
