# 知微智学

> AI-powered knowledge graph automatic generation system for education

**知微智学** 是一个面向教育的 AI 知识图谱自动生成平台。教师上传课件材料（PDF/Word/PPT），系统通过 DeepSeek 大模型自动提取知识节点和关系，生成可视化知识图谱。教师可编辑、发布图谱，学生通过图谱浏览知识点并获取关联学习资源。

## 核心流程

```
教师创建课程 → 上传课件 → AI 提取知识节点/关系 → 生成图谱 → 教师编辑发布 → 学生浏览学习
```

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.3 + Java 17 |
| 数据库 | MySQL (JPA) + Neo4j Aura (图谱存储) |
| 认证 | Spring Security + JWT |
| AI 引擎 | DeepSeek API (v4-pro) |
| 前端 | 原生 HTML/CSS/JS + ECharts 5.x |
| 构建 | Maven |

## 功能进度

### ✅ 已完成

- **用户系统**：邮箱验证码注册/登录、JWT 认证、PPT/文档页面
- **课程管理**：课程 CRUD、发布/取消发布
- **资源管理**：文件上传（PDF/Word/PPT）、文本提取、文件下载
- **图谱生成**：DeepSeek 提取知识节点/关系 → Neo4j 持久化、课程级隔离
- **图谱编辑**：节点/关系 CRUD API、前端 ECharts 树形可视化
- **学生端**：已发布课程浏览、图谱探索、课程资源列表、节点详情页
- **AI 代理**：后端代理 AI 请求（助教、出题），不暴露 API key

### 🔶 进行中

- 学生端沉浸式学习页面完善
- 图谱布局优化（解决节点重叠、拥挤问题）

### ⏳ 计划中

- 大文档分块提取与图谱合并
- 学习进度记录
- 部署到服务器

## 快速开始

### 环境要求

- JDK 17+
- MySQL / MariaDB
- Neo4j (可使用 Neo4j Aura 云端实例)
- DeepSeek API Key

### 配置

复制 `application-local.example.properties` 为 `application-local.properties`，填入：

```properties
deepseek.api.key=your_api_key
spring.datasource.url=jdbc:mysql://localhost:3306/kgc_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.neo4j.uri=neo4j+s://your-instance.databases.neo4j.io
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=your_password
```

### 运行

```bash
./mvnw spring-boot:run
```

访问 `http://localhost:8080` 即可使用。

## 项目结构

```
src/main/java/com/wyj/kgc/
├── config/          # Spring 配置
├── controller/      # REST API 控制器
├── dto/             # 数据传输对象
├── entity/          # JPA/Neo4j 实体
├── repository/      # 数据访问层
├── security/        # JWT 认证
├── service/         # 业务逻辑
│   └── kg/          # 知识图谱服务
└── utils/           # 工具类（PDF/Word/PPT 解析）

src/main/resources/static/  # 前端页面
```

## API 概览

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/**` | POST | 注册、登录、验证码 |
| `/api/v1/courses/**` | CRUD | 课程管理、发布 |
| `/api/v1/files/**` | GET/POST/DELETE | 文件上传、下载、列表 |
| `/api/v1/graph/**` | GET/POST/PUT/DELETE | 图谱节点/关系操作 |
| `/api/v1/ai/**` | POST | AI 代理（助教、出题） |

## 作者

吴育锦 · 湘潭大学 · 创新创业项目

---

*最后更新：2026-07-30 ·*
