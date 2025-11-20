# Text2SQL Service

基于 Spring AI 和 DeepSeek 大模型构建的自然语言转 SQL 服务。支持多数据源管理、Schema 自动发现、SQL 生成与安全验证。

## 核心功能

- **自然语言转 SQL**: 将用户输入的自然语言问题转换为可执行的 MySQL 查询语句。
- **智能 Schema 发现**: 自动获取并缓存数据库表结构、字段注释和外键关系，为大模型提供准确的上下文。
- **多模式生成**: 结合规则匹配（Few-shot）和 AI 生成，支持用户反馈学习。
- **安全验证流水线**:
  - 基于 JSqlParser 的 AST 语法分析，严格限制仅允许 `SELECT` 查询。
  - 深度检查 SQL 注入风险和危险操作（如 `DROP`, `DELETE`）。
- **MCP (Model Context Protocol)**: 支持通过 MCP 协议扩展上下文能力（如文件系统、GitHub）。

## 技术栈

- **Java 21** (LTS)
- **Spring Boot 3.4.1**
- **Spring AI 1.1.0**
- **大模型**: DeepSeek V3 (兼容 OpenAI 接口)
- **数据库**: MySQL 8.3, H2 (内置)
- **连接池**: HikariCP
- **工具库**: JSqlParser (SQL 解析), Lombok

## 快速开始

### 前置要求

- JDK 21+
- MySQL 8.0+
- DeepSeek API Key

### 配置

1. 复制环境变量示例文件：
   ```bash
   cp .env.example .env
   ```

2. 修改 `.env` 文件或设置环境变量：
   ```properties
   DEEPSEEK_API_KEY=sk-your-api-key-here
   # 可选：配置默认数据库连接
   # MYSQL_HOST=localhost
   # MYSQL_PORT=3306
   # MYSQL_USERNAME=root
   # MYSQL_PASSWORD=password
   ```

### 运行

**方式一：使用启动脚本（推荐）**

```bash
# 设置 API Key
export DEEPSEEK_API_KEY=sk-your-api-key-here

# 运行启动脚本
./start.sh
```

**方式二：直接运行**

```bash
# 设置环境变量并启动
DEEPSEEK_API_KEY=sk-your-api-key-here ./gradlew bootRun

# 或者先导出环境变量
export DEEPSEEK_API_KEY=sk-your-api-key-here
./gradlew bootRun
```

应用启动后访问：`http://localhost:8090`。

## 架构说明

1. **SchemaDiscoveryService**: 负责连接数据库并提取元数据。
2. **SemanticAnalyzer**: 分析用户查询意图和实体。
3. **DualPatternManager**: 尝试匹配历史成功模式（RAG）。
4. **ContextualPromptBuilder**: 构建包含 Schema 信息和 Few-shot 示例的 Prompt。
5. **ChatClient**: 调用 DeepSeek API 生成 SQL。
6. **SqlValidationPipeline**: 对生成的 SQL 进行语法、安全和语义验证。

## 安全说明

本项目专为只读查询设计。虽然内置了严格的 SQL 验证机制，但建议在生产环境中配置只读的数据库账号（仅授予 `SELECT` 权限）以确保绝对安全。
