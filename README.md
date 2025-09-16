# Spring AI MCP 项目

## 项目简介

基于Spring AI和Java 24构建的Model Context Protocol (MCP) 集成项目。该项目展示了如何使用最新的Java特性（虚拟线程、结构化并发）与Spring AI MCP进行集成。

## 技术栈

- **Java 24** - 使用最新的Java特性
- **Spring Boot 3.4.0** - 现代化的Spring框架
- **Spring AI 1.0.0-M4** - Spring生态的AI集成
- **虚拟线程** - Java 21+的轻量级并发模型
- **结构化并发** - Java 24的新并发编程模式

## Java 24新特性应用

### 1. 虚拟线程 (Virtual Threads)
- 在MCP客户端通信中使用虚拟线程，提升并发性能
- 配置专用的虚拟线程执行器处理I/O密集型任务

### 2. 结构化并发 (Structured Concurrency)
- 使用`StructuredTaskScope`同时执行多个MCP任务
- 统一错误处理和任务生命周期管理

### 3. 作用域值 (Scoped Values)
- 在并发任务间安全共享数据

## 功能特性

- ✅ 文件系统MCP服务器集成
- ✅ GitHub MCP服务器集成  
- ✅ 异步任务执行支持
- ✅ 结构化并发多任务处理
- ✅ RESTful API接口
- ✅ 健康检查和状态监控
- ✅ 虚拟线程优化

## API端点

### MCP任务执行
- `POST /api/mcp/filesystem` - 执行文件系统相关任务
- `POST /api/mcp/github` - 执行GitHub相关任务
- `POST /api/mcp/multiple` - 使用结构化并发执行多个任务

### 系统状态
- `GET /api/mcp/status` - 获取MCP连接状态
- `GET /api/mcp/health` - 健康检查

## 配置要求

### 环境变量
```bash
# OpenAI API密钥
export OPENAI_API_KEY=your_openai_api_key

# GitHub访问令牌（可选）
export GITHUB_TOKEN=your_github_token
```

### 前置条件
- Node.js (用于MCP服务器)
- Java 24
- 相关MCP服务器包：
  ```bash
  npm install -g @modelcontextprotocol/server-filesystem
  npm install -g @modelcontextprotocol/server-github
  ```

## 运行项目

1. **构建项目**
   ```bash
   ./gradlew build
   ```

2. **运行应用**
   ```bash
   ./gradlew bootRun --args='--enable-preview'
   ```

3. **访问应用**
   - 应用地址: http://localhost:8080
   - 健康检查: http://localhost:8080/api/mcp/health

## 请求示例

### 文件系统任务
```bash
curl -X POST http://localhost:8080/api/mcp/filesystem \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "列出/tmp目录下的所有文件",
    "async": false
  }'
```

### GitHub任务
```bash
curl -X POST http://localhost:8080/api/mcp/github \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "获取我的GitHub仓库列表",
    "async": false
  }'
```

### 多任务执行
```bash
curl -X POST "http://localhost:8080/api/mcp/multiple?filesystemPrompt=检查系统状态&githubPrompt=获取最新提交"
```

## 项目结构

```
src/main/java/com/kami/springai/mcp/
├── SpringAiMcpApplication.java    # 主应用类
├── config/
│   ├── McpConfig.java            # MCP客户端配置
│   └── ConcurrencyConfig.java    # 并发配置
├── controller/
│   └── McpController.java        # REST控制器
├── service/
│   └── McpService.java          # MCP业务逻辑
└── model/
    ├── McpRequest.java          # 请求模型
    └── McpResponse.java         # 响应模型
```

## 开发说明

该项目作为Spring AI MCP集成的项目雏形，可以在此基础上扩展更多功能：

- 添加更多MCP服务器集成
- 实现任务队列和批处理
- 增加监控和指标收集  
- 扩展API功能
- 添加前端界面

## 许可证

MIT License