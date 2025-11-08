# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供在此代码库中工作时的指导。

## 项目概述

Spring AI MCP (Model Context Protocol) 集成项目，具备 Text2SQL 自然语言转SQL能力。基于 Spring Boot 3.4.1 和 Java 24 构建，充分利用现代并发特性（虚拟线程、结构化并发）实现AI驱动的自然语言到SQL转换。

**核心功能：**
- 使用 DeepSeek AI 进行自然语言到SQL转换
- 动态数据库结构发现和缓存
- 基于用户反馈的AI学习框架
- 包含语义分析的SQL验证流水线
- MCP服务器集成（文件系统、GitHub、SQLite）
- Vue.js 前端交互式查询界面

## 构建和运行

### 前置要求
- Java 24（使用预览特性）
- Node.js（用于MCP服务器）
- MySQL 8.4.0+
- 环境变量：`DEEPSEEK_API_KEY`、`MYSQL_*` 数据库凭证

### 构建命令
```bash
# 构建项目
./gradlew build

# 运行应用（自动包含 --enable-preview）
./gradlew bootRun

# 运行测试
./gradlew test

# 运行单个测试
./gradlew test --tests "类名.方法名"
```

### 访问入口
- 应用主页：http://localhost:8090
- 健康检查：http://localhost:8090/api/mcp/health
- 主界面：http://localhost:8090/index.html

## 架构设计

### Java 24 特性应用

**虚拟线程（Virtual Threads）：**
- 所有I/O密集型操作（MCP通信、数据库查询）使用虚拟线程执行器
- 配置位置：`ConcurrencyConfig.java` 中的 `virtualThreadExecutor`、`mcpExecutor`
- 启用方式：`spring.threads.virtual.enabled=true`

**作用域值（ScopedValue，计划用于多数据源）：**
- 将替代ThreadLocal用于多数据源上下文管理
- 兼容层：ScopedValue（虚拟线程）+ ThreadLocal（降级）

**结构化并发（Structured Concurrency）：**
- 用于 `McpController` 中的并行MCP任务执行
- 参考 `executeMultipleTasks()` 方法

### 模块结构

```
com.kami.springai/
├── mcp/                          # MCP集成模块
│   ├── config/                   # MCP客户端和并发配置
│   ├── controller/               # REST端点
│   ├── service/                  # MCP业务逻辑
│   └── model/                    # 请求/响应DTO
├── text2sql/                     # Text2SQL转换模块
│   ├── service/
│   │   ├── Text2SqlService       # 主转换编排器
│   │   ├── SchemaDiscoveryService # 数据库元数据提取
│   │   ├── SqlExecutionService   # 安全的SQL执行
│   │   ├── SemanticAnalyzer      # 查询意图分析
│   │   └── GeneralizedLearner    # 从反馈中学习
│   ├── config/
│   │   ├── YamlConfigManager     # 模式存储（基础+学习）
│   │   └── ContextualPromptBuilder # AI提示词工程
│   ├── validation/               # SQL验证流水线
│   └── model/                    # 语义模型（意图、实体、条件）
└── common/
    └── cache/                    # Schema缓存（TTL: 1小时）
```

### Text2SQL 转换流程

```
用户查询 → 语义分析器 → 模式匹配（YamlConfigManager）
    ↓ （无匹配）
    AI生成（DeepSeek） → SQL清理 → 验证流水线
    ↓ （失败）
    重试改进（最多3次）
    ↓ （成功）
    执行 → 返回结果 → 从反馈学习
```

**关键类：**
- `Text2SqlService.convertToSql()`：主入口（455行）
- `SqlValidationPipeline`：语法/语义/安全/性能验证
- `SchemaCache`：Redis支持的schema缓存
- `YamlConfigManager`：双模式存储（base-patterns.yml + generalized-patterns.yml）

### 数据库连接

**当前状态：** 单数据源配置在 `application.yml` 中
- HikariCP连接池：最小5个、最大20个连接
- 环境变量覆盖：`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`

**计划中的多数据源功能：**
- 完整设计见 `多数据源管理技术方案.md`
- 将通过Web界面添加动态数据源管理
- 使用 ScopedValue/ThreadLocal 实现用户隔离

### 重要约束

**安全性：**
- **只读模式：** 仅允许SELECT查询（在 `Text2SqlService.validateGeneratedSql()` 中强制执行）
- 禁止关键字：INSERT、UPDATE、DELETE、DROP、ALTER、CREATE、TRUNCATE、REPLACE、MERGE、CALL、EXEC
- 最大查询行数：1000（可在 `text2sql.security.max-rows` 配置）
- SQL长度限制：3000字符
- 禁止多语句执行

**AI配置：**
- 模型：DeepSeek Chat（OpenAI兼容API）
- Temperature: 0.7，Max tokens: 4096
- 系统提示词强制仅输出SELECT语句
- 输出清理：移除markdown代码块、确保分号结尾

### 配置文件

**YAML模式文件（位于 `src/main/resources/config/`）：**
- `base-patterns.yml`：只读的基础SQL模式
- `generalized-patterns.yml`：用户学习的模式（运行时更新）
- `learning-history.yml`：反馈历史记录
- `enhanced-entity-semantics.yml`：实体类型定义
- `intelligent-prompt-templates.yml`：AI提示词模板
- `database-optimization-config.yml`：查询优化提示

**应用配置：**
- `application.yml`：主配置文件（108行）
- 环境变量：`.env.example` 模板

## 开发指南

### 添加新的API端点

控制器位于 `mcp/controller/`，遵循以下模式：
```java
@PostMapping("/api/mcp/endpoint")
public McpResponse handleRequest(@RequestBody McpRequest request) {
    // 对I/O操作使用异步执行器
    // 返回结构化的 McpResponse
}
```

### 修改 Text2SQL 逻辑

**调整SQL生成：**
1. 编辑 `Text2SqlService.SYSTEM_PROMPT` 中的系统提示词
2. 修改 `SemanticAnalyzer` 中的语义分析
3. 在 `validator/` 包中添加验证规则

**添加学习模式：**
- 模式通过用户反馈自动保存到 `generalized-patterns.yml`
- 手动添加：遵循 `base-patterns.yml` 中的YAML结构

### 使用虚拟线程

**应该做的：**
- 对I/O任务使用 `@Async("virtualThreadExecutor")`
- 对CPU密集型任务使用 `@Async("cpuIntensiveExecutor")`
- 确保阻塞操作在虚拟线程上下文中

**不应该做的：**
- 在虚拟线程代码中使用 synchronized 块（使用 ReentrantLock）
- 为I/O操作创建传统线程池

### 数据库Schema变更

当schema发生变化时：
1. 缓存在1小时后自动过期（`text2sql.cache.schema-ttl`）
2. 手动清除：重启应用或实现缓存清除端点
3. Schema发现通过JDBC元数据自动进行

### 测试SQL生成

使用 `/api/mcp/text2sql` 端点：
```bash
curl -X POST http://localhost:8090/api/mcp/text2sql \
  -H "Content-Type: application/json" \
  -d '{"query": "查询所有用户"}'
```

响应包括：
- 生成的SQL
- 执行结果
- 验证警告

### Gradle 注意事项

**Java 24 编译：**
- 所有任务自动包含 `--enable-preview`
- 需要模块访问的JVM参数（见 `build.gradle` 第73-92行）
- Lombok使用edge-SNAPSHOT以兼容Java 24

**常见问题：**
- 如果Lombok失败：确保获取了 `org.projectlombok:lombok:edge-SNAPSHOT`
- 如果测试因模块错误失败：检查 `build.gradle` 中的 `--add-opens` 标志

## 修改前需审查的关键文件

**核心服务：**
- `Text2SqlService.java`（455行）- 包含重试/验证的主转换逻辑
- `SchemaDiscoveryService.java`（170行）- JDBC元数据提取
- `SqlExecutionService.java` - 安全的SQL执行
- `McpController.java`（317行）- 所有REST端点

**配置：**
- `application.yml` - 运行时配置
- `ConcurrencyConfig.java` - 虚拟线程执行器
- `base-patterns.yml` - SQL模式模板

**前端：**
- `static/index.html` - Vue.js主界面
- `static/js/app.js` - 前端逻辑（数据库选择器、查询执行、反馈）

## 即将推出的功能

参见 `多数据源管理技术方案.md` 了解计划中的多数据源架构：
- 通过Web界面动态创建数据源
- 使用 ScopedValue 实现每用户数据源隔离
- 基于JSON的数据源持久化（AES-256密码加密）
- 新增API端点：`/api/datasources/*`


## 开发习惯
- 编码思想遵守高内聚低耦合、每个方法单一职责、善用抽象
- 代码可读性好
- 使用`@RequiredArgsConstructor`注解来注入spring bean

## 命名规则
- 参数是集合或者数组使用后缀list或者s
- 参数命名见名知意