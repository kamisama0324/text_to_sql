# SpringAI-MCP API 接口文档

## 接口概览

SpringAI-MCP 提供了完整的 RESTful API 接口，支持自然语言到 SQL 的转换、数据库操作、MCP 集成等功能。

## 1. Text2SQL 核心接口

### 1.1 自然语言转 SQL

**接口地址**: `POST /api/mcp/text2sql/text-to-sql`

**功能描述**: 将自然语言查询转换为安全的 SQL 语句

**请求参数**:
```http
Content-Type: application/x-www-form-urlencoded

query=查询所有用户信息
database=test  (可选)
```

**响应示例**:
```json
{
  "success": true,
  "result": "**生成的SQL语句:**\n```sql\nSELECT * FROM `echoing_user` LIMIT 100;\n```\n\n**查询说明:**\n查询echoing_user表的前100条用户记录...",
  "error": null,
  "mcp_type": "text_to_sql",
  "execution_time_ms": 13024,
  "timestamp": "2025-09-11T15:28:09.618668",
  "thread_info": "VirtualThread[#68,tomcat-handler-2]/runnable@ForkJoinPool-1-worker-1"
}
```

**性能指标**:
- 首次查询: ~14秒 (包含数据库结构发现)
- 缓存命中: ~13秒 (使用缓存的数据库结构)

### 1.2 SQL 解释

**接口地址**: `POST /api/mcp/text2sql/explain-sql`

**功能描述**: 为 SQL 语句提供自然语言解释

**请求参数**:
```http
Content-Type: application/x-www-form-urlencoded

sql=SELECT * FROM users WHERE age > 18
database=test  (可选)
```

### 1.3 SQL 执行

**接口地址**: `POST /api/mcp/text2sql/execute-query`

**功能描述**: 安全执行 SQL 查询并返回结果

**请求参数**:
```http
Content-Type: application/x-www-form-urlencoded

sql=SELECT id, name FROM users LIMIT 10
database=test  (可选)
```

**响应示例**:
```json
{
  "success": true,
  "result": {
    "columns": ["id", "name"],
    "rows": [
      [1, "张三"],
      [2, "李四"]
    ],
    "totalRows": 2,
    "executionTime": 156,
    "truncated": false
  },
  "error": null
}
```

### 1.4 一体化查询执行

**接口地址**: `POST /api/mcp/text2sql/query-and-execute`

**功能描述**: 自然语言转 SQL 并直接执行，返回查询结果

**请求参数**:
```http
Content-Type: application/x-www-form-urlencoded

query=查询前10个用户的姓名和邮箱
database=test  (可选)
```

## 2. 数据库管理接口

### 2.1 数据库连接测试

**接口地址**: `GET /api/mcp/text2sql/test-connection`

**功能描述**: 测试数据库连接状态

**响应示例**:
```json
{
  "success": true,
  "result": "数据库连接正常，当前数据库: test",
  "error": null,
  "mcp_type": "test_connection",
  "execution_time_ms": 8,
  "timestamp": "2025-09-11T12:01:14.518691",
  "thread_info": "VirtualThread[#145,tomcat-handler-29]/runnable@ForkJoinPool-1-worker-18"
}
```

### 2.2 数据库结构描述

**接口地址**: `POST /api/mcp/text2sql/describe-database`

**功能描述**: 获取数据库结构的详细描述

**请求参数**:
```http
Content-Type: application/x-www-form-urlencoded

database=test  (可选)
```

**响应示例**:
```json
{
  "success": true,
  "result": {
    "databaseName": "test",
    "tables": [...],
    "totalTables": 121,
    "totalColumns": 1766,
    "discoveryTime": 571
  }
}
```

### 2.3 权限检查

**接口地址**: `GET /api/mcp/text2sql/check-permissions`

**功能描述**: 检查当前数据库连接的权限

## 3. 学习系统接口

### 3.1 反馈学习

**接口地址**: `POST /api/mcp/text2sql/learn-from-feedback`

**功能描述**: 基于用户反馈改进 SQL 生成质量

**请求参数**:
```http
Content-Type: application/x-www-form-urlencoded

originalQuery=查询用户信息
generatedSql=SELECT * FROM echoing_user LIMIT 100
feedbackType=positive
userCorrection=  (可选)
```

### 3.2 学习统计

**接口地址**: `GET /api/mcp/text2sql/learning-stats`

**功能描述**: 获取学习系统的统计信息

**响应示例**:
```json
{
  "success": true,
  "result": "# 学习系统统计信息\n\n## 模式统计\n- **泛化SQL模式:** 0 个\n- **实体语义模式:** 1 个\n...",
  "error": null,
  "mcp_type": "learning_stats",
  "execution_time_ms": 27
}
```

### 3.3 增强转换

**接口地址**: `POST /api/mcp/text2sql/enhanced-text-to-sql`

**功能描述**: 使用学习系统增强的 SQL 转换

## 4. MCP 集成接口

### 4.1 MCP 服务状态

**接口地址**: `GET /api/mcp/status`

**功能描述**: 获取 MCP 服务状态信息

**响应示例**:
```json
{
  "success": true,
  "result": "Spring AI MCP服务状态:\n- 虚拟线程支持: ✅\n- Java版本: 24.0.2\n- 当前线程: VirtualThread[#136,tomcat-handler-22]/runnable@ForkJoinPool-1-worker-16\n- 服务状态: 正常运行",
  "error": null,
  "mcp_type": "status",
  "execution_time_ms": 1
}
```

### 4.2 文件系统任务

**接口地址**: `POST /api/mcp/filesystem`

**功能描述**: 执行文件系统相关的 MCP 任务

**请求参数**:
```json
{
  "prompt": "列出当前目录文件",
  "async": false
}
```

### 4.3 GitHub 任务

**接口地址**: `POST /api/mcp/github`

**功能描述**: 执行 GitHub 相关的 MCP 任务

### 4.4 多任务并行执行

**接口地址**: `POST /api/mcp/multiple`

**功能描述**: 使用结构化并发执行多个 MCP 任务

**请求参数**:
```http
Content-Type: application/x-www-form-urlencoded

filesystemPrompt=读取配置文件
githubPrompt=获取仓库信息
```

### 4.5 健康检查

**接口地址**: `GET /api/mcp/health`

**功能描述**: MCP 服务健康检查

**响应**:
```
Spring AI MCP服务运行正常
```

## 5. 系统监控接口

### 5.1 缓存状态

**接口地址**: `GET /api/health/cache`

**功能描述**: 获取缓存系统状态

**响应示例**:
```json
{
  "status": "UP",
  "local_cache_size": 1,
  "cache_enabled": true,
  "redis_connected": false,
  "cache_stats": "CacheStats{本地缓存: 1, Redis连接: 否, 缓存启用: 是}"
}
```

### 5.2 系统健康检查

**接口地址**: `GET /api/health`

**功能描述**: 整体系统健康状态检查

### 5.3 MCP 集成测试

**接口地址**: `GET /api/health/test-text2sql`

**功能描述**: 测试 Text2SQL 集成功能

## 6. 前端页面路由

### 6.1 主页

**路由**: `GET /`
**描述**: 重定向到主应用界面

### 6.2 演示页面

**路由**: `GET /demo`
**描述**: 演示功能页面

### 6.3 Text2SQL 界面

**路由**: `GET /text2sql`
**描述**: Text2SQL 专用界面

## 接口规范

### 通用响应格式

所有 API 接口都遵循统一的响应格式:

```json
{
  "success": true|false,
  "result": "具体结果数据",
  "error": null|"错误信息",
  "mcp_type": "接口类型标识",
  "execution_time_ms": 123,
  "timestamp": "2025-09-11T15:28:09.618668",
  "thread_info": "线程信息(用于调试)"
}
```

### 错误处理

#### 常见错误类型

1. **输入验证错误** (400)
   ```json
   {
     "success": false,
     "error": "Required request parameter 'query' is not present"
   }
   ```

2. **SQL 安全错误** (400)
   ```json
   {
     "success": false,
     "error": "SQL包含不允许的操作: DELETE"
   }
   ```

3. **AI 服务错误** (500)
   ```json
   {
     "success": false,
     "error": "SQL生成失败: AI服务暂时不可用"
   }
   ```

4. **数据库连接错误** (500)
   ```json
   {
     "success": false,
     "error": "数据库连接失败"
   }
   ```

### 安全限制

#### SQL 执行限制
- 只允许 SELECT 语句
- 禁止 DDL 操作 (CREATE, DROP, ALTER)
- 禁止 DML 操作 (INSERT, UPDATE, DELETE)
- 查询超时限制: 30秒
- 结果集限制: 1000行

#### 请求限制
- 查询字符串长度: 最大 1000 字符
- SQL 语句长度: 最大 3000 字符
- 并发请求: 基于虚拟线程，理论上无限制

### 性能优化

#### 缓存策略
- 数据库结构缓存: 3600秒 TTL
- 查询结果缓存: 300秒 TTL
- 本地缓存优先，Redis 分布式缓存备选

#### 响应优化
- 使用虚拟线程处理请求
- 结构化并发处理多任务
- 智能缓存减少数据库访问

## 客户端集成示例

### JavaScript/Fetch

```javascript
// Text2SQL 转换
async function convertToSql(query) {
    const response = await fetch('/api/mcp/text2sql/text-to-sql', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `query=${encodeURIComponent(query)}`
    });
    
    return await response.json();
}

// 执行查询
async function executeQuery(sql) {
    const response = await fetch('/api/mcp/text2sql/execute-query', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `sql=${encodeURIComponent(sql)}`
    });
    
    return await response.json();
}
```

### curl 示例

```bash
# 自然语言转 SQL
curl -X POST "http://localhost:8080/api/mcp/text2sql/text-to-sql" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "query=查询所有用户信息"

# 检查缓存状态
curl -X GET "http://localhost:8080/api/health/cache"

# 测试数据库连接
curl -X GET "http://localhost:8080/api/mcp/text2sql/test-connection"
```

---

*最后更新: 2025-09-11*