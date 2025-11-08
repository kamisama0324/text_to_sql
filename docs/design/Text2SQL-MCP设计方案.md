# Text2SQL MCP 设计方案

## 📋 项目概述

基于SpringAI MCP框架的Text2SQL智能查询系统，支持MySQL数据库的自然语言到SQL转换，具备自适应表结构支持能力。

### 核心特性
- 🚀 **自然语言转SQL**: 支持中英文自然语言查询
- 🔄 **自适应数据库**: 无需额外开发即可支持任何MySQL表结构
- ⚡ **实时性能**: 基于Java 24虚拟线程和结构化并发
- 🛡️ **安全可靠**: 多层安全防护和SQL注入防护
- 📊 **智能缓存**: 多级缓存策略优化查询性能

---

## 🏗️ 技术架构

### 系统架构图
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client Apps   │    │   Spring AI     │    │   MySQL         │
│                 │───▶│   MCP Server    │───▶│   Database      │
│ - Web UI        │    │                 │    │                 │
│ - API Client    │    │ - Text2SQL      │    │ - Schema Info   │
│ - CLI Tools     │    │ - Query Engine  │    │ - Data Storage  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                         ┌─────────────────┐
                         │   Cache Layer   │
                         │                 │
                         │ - Redis Cache   │
                         │ - Local Cache   │
                         │ - Schema Cache  │
                         └─────────────────┘
```

### 技术栈
- **框架**: Spring Boot 3.4.0 + Spring AI 1.0.0
- **语言**: Java 24 (虚拟线程 + 结构化并发)
- **数据库**: MySQL 8.0+
- **缓存**: Redis + Caffeine
- **协议**: Model Context Protocol (MCP)
- **AI集成**: OpenAI GPT-4 / 本地LLM

---

## 🔧 核心功能模块

### 1. Text2SQL引擎 (`Text2SqlEngine`)
```java
public class Text2SqlEngine {
    // 自然语言解析
    public ParsedQuery parseNaturalLanguage(String query);
    
    // SQL生成
    public GeneratedSql generateSql(ParsedQuery parsed, DatabaseSchema schema);
    
    // 查询优化
    public OptimizedSql optimizeQuery(GeneratedSql sql);
}
```

### 2. 数据库适配器 (`DatabaseAdapter`)
```java
public class DatabaseAdapter {
    // 元数据发现
    public DatabaseSchema discoverSchema(String database);
    
    // 查询执行
    public QueryResult executeQuery(String sql, QueryParams params);
    
    // 连接管理
    public void manageConnections();
}
```

### 3. 缓存管理器 (`CacheManager`)
```java
public class CacheManager {
    // 模式缓存
    public void cacheSchema(String database, DatabaseSchema schema);
    
    // 查询缓存
    public void cacheQueryResult(String queryHash, QueryResult result);
    
    // 缓存失效
    public void invalidateCache(String database);
}
```

### 4. 安全控制器 (`SecurityController`)
```java
public class SecurityController {
    // SQL验证
    public ValidationResult validateSql(String sql);
    
    // 权限检查
    public boolean checkPermissions(User user, String operation);
    
    // 注入防护
    public String sanitizeSql(String sql);
}
```

---

## 🛠️ MCP工具定义

### 工具清单
| 工具名称 | 功能描述 | 输入参数 | 返回结果 |
|---------|---------|---------|---------|
| `text_to_sql` | 自然语言转SQL | query, database | SQL语句 |
| `execute_query` | 执行SQL查询 | sql, params | 查询结果 |
| `describe_database` | 获取数据库结构 | database | 表结构信息 |
| `list_tables` | 列出所有表 | database | 表名列表 |
| `explain_sql` | 解释SQL执行计划 | sql | 执行计划 |
| `validate_query` | 验证SQL语法 | sql | 验证结果 |

### MCP工具实现示例
```json
{
  "name": "text_to_sql",
  "description": "Convert natural language to SQL query",
  "inputSchema": {
    "type": "object",
    "properties": {
      "query": {
        "type": "string",
        "description": "Natural language query"
      },
      "database": {
        "type": "string", 
        "description": "Target database name"
      }
    },
    "required": ["query", "database"]
  }
}
```

---

## 🔄 数据库变更管理方案

### 智能变更检测
- **实时监控**: 定期检测数据库结构变更（表、字段、关系）
- **版本指纹**: 基于结构哈希快速识别变更
- **增量分析**: 精确定位具体变更项目和影响范围

### 一键同步功能
- **自动更新**: 高置信度变更（如新增表/字段）自动处理
- **智能确认**: 低置信度变更提供建议选项供确认
- **批量处理**: 支持多数据库并发同步
- **安全回滚**: 变更前自动备份，支持一键回滚

### 变更影响分析
- **查询影响**: 分析变更对现有查询的潜在影响
- **API影响**: 评估对Text2SQL API的影响
- **性能影响**: 预测变更对系统性能的影响
- **风险评估**: 自动评估变更风险等级

### 操作界面
- **Web管理界面**: 可视化变更检测和同步操作
- **CLI工具**: 支持脚本化的批量操作
- **REST API**: 完整的API支持集成到CI/CD流程

---

## 🔄 自适应数据库支持方案

### 元数据自动发现
```sql
-- 1. 获取数据库中所有表
SELECT 
    TABLE_NAME,
    TABLE_TYPE,
    TABLE_COMMENT,
    CREATE_TIME,
    UPDATE_TIME
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = ?

-- 2. 获取表字段信息
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT,
    COLUMN_KEY,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?

-- 3. 获取外键关系
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL
```

### 智能表结构分析
```java
public class SchemaAnalyzer {
    
    // 分析表关系
    public TableRelationship analyzeRelationships(DatabaseSchema schema) {
        // 1. 基于外键的显式关系
        Map<String, List<ForeignKey>> foreignKeys = extractForeignKeys(schema);
        
        // 2. 基于命名约定的隐式关系
        Map<String, List<ImplicitRelation>> implicitRelations = 
            inferRelationsByNaming(schema);
        
        // 3. 基于数据类型的关联推断
        Map<String, List<TypeRelation>> typeRelations = 
            inferRelationsByType(schema);
            
        return new TableRelationship(foreignKeys, implicitRelations, typeRelations);
    }
    
    // 生成表描述
    public TableDescription generateTableDescription(Table table) {
        StringBuilder desc = new StringBuilder();
        desc.append("表 ").append(table.getName());
        
        if (StringUtils.isNotBlank(table.getComment())) {
            desc.append("(").append(table.getComment()).append(")");
        }
        
        desc.append(" 包含以下字段:\n");
        
        for (Column column : table.getColumns()) {
            desc.append("- ").append(column.getName())
                .append("(").append(column.getType()).append(")");
            
            if (StringUtils.isNotBlank(column.getComment())) {
                desc.append(": ").append(column.getComment());
            }
            desc.append("\n");
        }
        
        return new TableDescription(desc.toString());
    }
}
```

### 缓存策略
```java
@Service
public class SchemaCache {
    
    private final Cache<String, DatabaseSchema> localCache;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 分层缓存策略
    public DatabaseSchema getSchema(String database) {
        // L1: 本地缓存
        DatabaseSchema schema = localCache.getIfPresent(database);
        if (schema != null) {
            return schema;
        }
        
        // L2: Redis缓存
        schema = (DatabaseSchema) redisTemplate.opsForValue()
            .get("schema:" + database);
        if (schema != null) {
            localCache.put(database, schema);
            return schema;
        }
        
        // L3: 数据库查询
        schema = databaseAdapter.discoverSchema(database);
        
        // 缓存更新
        redisTemplate.opsForValue().set("schema:" + database, schema, 
            Duration.ofHours(24));
        localCache.put(database, schema);
        
        return schema;
    }
    
    // 智能缓存刷新
    @Scheduled(fixedRate = 3600000) // 每小时检查一次
    public void refreshSchemaCache() {
        Set<String> databases = getActiveDatabases();
        
        for (String database : databases) {
            CompletableFuture.runAsync(() -> {
                DatabaseSchema currentSchema = databaseAdapter.discoverSchema(database);
                DatabaseSchema cachedSchema = getSchema(database);
                
                if (!Objects.equals(currentSchema.getVersion(), cachedSchema.getVersion())) {
                    log.info("检测到数据库 {} 结构变更，更新缓存", database);
                    updateCache(database, currentSchema);
                }
            });
        }
    }
}
```

---

## 🛡️ 安全机制设计

### 1. SQL注入防护
```java
@Component
public class SqlSecurityValidator {
    
    private final List<String> DANGEROUS_KEYWORDS = Arrays.asList(
        "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE"
    );
    
    public ValidationResult validateSql(String sql) {
        // 1. 关键词检查
        String upperSql = sql.toUpperCase();
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                return ValidationResult.error("检测到危险操作: " + keyword);
            }
        }
        
        // 2. 语法解析验证
        try {
            CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            return ValidationResult.error("SQL语法错误: " + e.getMessage());
        }
        
        // 3. 查询复杂度检查
        if (countSubqueries(sql) > 3) {
            return ValidationResult.warning("查询过于复杂，可能影响性能");
        }
        
        return ValidationResult.success();
    }
}
```

### 2. 权限控制
```java
@Component
public class DatabasePermissionManager {
    
    @Value("${text2sql.security.readonly:true}")
    private boolean readOnlyMode;
    
    public boolean checkQueryPermission(String database, String sql, User user) {
        // 1. 只读模式检查
        if (readOnlyMode && !isSelectQuery(sql)) {
            return false;
        }
        
        // 2. 数据库访问权限
        if (!user.hasAccessTo(database)) {
            return false;
        }
        
        // 3. 表级别权限
        List<String> tables = extractTablesFromSql(sql);
        for (String table : tables) {
            if (!user.hasTableAccess(database, table)) {
                return false;
            }
        }
        
        return true;
    }
}
```

### 3. 查询限制
```java
@Component
public class QueryLimiter {
    
    @Value("${text2sql.query.maxRows:1000}")
    private int maxRows;
    
    @Value("${text2sql.query.timeout:30}")
    private int timeoutSeconds;
    
    public String addLimitation(String sql) {
        // 自动添加LIMIT子句
        if (!sql.toUpperCase().contains("LIMIT")) {
            sql += " LIMIT " + maxRows;
        }
        
        return sql;
    }
    
    public QueryResult executeWithTimeout(String sql, QueryParams params) {
        return CompletableFuture
            .supplyAsync(() -> databaseAdapter.executeQuery(sql, params))
            .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .join();
    }
}
```

---

## 🚀 实现计划

### 阶段一：基础框架搭建 (1周)
- [x] 项目结构设计
- [ ] 数据库连接配置
- [ ] MCP服务器集成
- [ ] 基础API定义
- [ ] 数据库自动发现模块

### 阶段二：核心功能开发 (2周)
- [ ] 元数据发现引擎
- [ ] Text2SQL转换引擎
- [ ] 查询执行引擎
- [ ] 缓存机制实现
- [ ] 变更检测机制

### 阶段三：智能化与自动化 (1.5周)
- [ ] 语义分析器
- [ ] 关系推断引擎
- [ ] 一键同步功能
- [ ] 交互式确认界面

### 阶段四：安全与优化 (1周)
- [ ] 安全防护机制
- [ ] 性能优化
- [ ] 错误处理完善
- [ ] 单元测试编写

### 阶段五：集成测试与部署 (1.5周)
- [ ] 集成测试编写
- [ ] 变更管理测试
- [ ] 性能测试
- [ ] 安全测试
- [ ] 部署文档完善

---

## 📦 部署指南

### 环境要求
- Java 24+
- MySQL 8.0+
- Redis 6.0+ (可选)
- Spring Boot 3.4.0+

### 配置示例
```yaml
spring:
  application:
    name: text2sql-mcp
  
  datasource:
    mysql:
      primary:
        url: jdbc:mysql://localhost:3306/testdb
        username: ${MYSQL_USERNAME}
        password: ${MYSQL_PASSWORD}
      secondary:
        url: jdbc:mysql://localhost:3306/analytics
        username: ${MYSQL_USERNAME} 
        password: ${MYSQL_PASSWORD}
  
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.1
    
    mcp:
      server:
        text2sql:
          enabled: true
          port: 8081

text2sql:
  security:
    readonly: true
    maxRows: 1000
    timeout: 30
  cache:
    enabled: true
    ttl: 3600
  databases:
    - name: testdb
      description: "测试数据库"
    - name: analytics  
      description: "分析数据库"
  
  # 变更管理配置
  schema-sync:
    enabled: true
    detection:
      interval: 300000  # 5分钟检测一次
      auto-update-threshold: 0.8
    backup:
      enabled: true
      retention-days: 30
    notifications:
      enabled: true
      webhook-url: "http://localhost:8080/api/schema-changes"
```

### 启动命令
```bash
# 开发环境
./gradlew bootRun --args='--enable-preview'

# 生产环境
java --enable-preview -jar text2sql-mcp.jar
```

---

## 📚 API文档

### REST API端点

#### 1. 自然语言转SQL
```http
POST /api/text2sql/generate
Content-Type: application/json

{
  "query": "查询销售额大于10000的客户",
  "database": "testdb",
  "options": {
    "limit": 100,
    "explain": true
  }
}
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "sql": "SELECT * FROM customers WHERE sales_amount > 10000 LIMIT 100",
    "explanation": "查询销售额大于10000的客户记录",
    "tables": ["customers"],
    "executionTime": 45
  }
}
```

#### 2. 执行查询
```http
POST /api/text2sql/execute
Content-Type: application/json

{
  "sql": "SELECT * FROM customers WHERE sales_amount > 10000 LIMIT 10",
  "database": "testdb",
  "params": {}
}
```

**响应示例:**
```json
{
  "success": true,
  "data": {
    "columns": ["id", "name", "sales_amount", "created_at"],
    "rows": [
      [1, "张三", 15000.0, "2024-01-01"],
      [2, "李四", 12000.0, "2024-01-02"]
    ],
    "count": 2,
    "executionTime": 15
  }
}
```

### MCP工具使用示例

#### 使用text_to_sql工具
```json
{
  "method": "tools/call",
  "params": {
    "name": "text_to_sql",
    "arguments": {
      "query": "Show me top 5 customers by revenue",
      "database": "sales_db"
    }
  }
}
```

#### 使用execute_query工具
```json
{
  "method": "tools/call", 
  "params": {
    "name": "execute_query",
    "arguments": {
      "sql": "SELECT customer_name, SUM(order_amount) as revenue FROM orders GROUP BY customer_name ORDER BY revenue DESC LIMIT 5",
      "database": "sales_db"
    }
  }
}
```

---

## 🔍 监控和日志

### 性能监控指标
- 查询响应时间
- SQL生成耗时
- 数据库连接池状态
- 缓存命中率
- 错误率统计

### 日志配置
```yaml
logging:
  level:
    com.kami.springai.mcp.text2sql: INFO
    org.springframework.ai: DEBUG
  pattern:
    console: '%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n'
  file:
    name: logs/text2sql-mcp.log
    max-size: 100MB
    max-history: 30
```

---

## 🎯 扩展计划

### 短期目标
- 支持更多数据库类型 (PostgreSQL, SQL Server)
- 增加更多自然语言处理能力
- 优化查询性能和缓存策略
- 完善Web管理界面

### 长期目标  
- 支持多租户架构
- 集成更多AI模型选择
- 支持复杂查询和分析
- 提供查询建议和优化建议

---

## 📖 参考资料

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/)
- [MySQL Information Schema](https://dev.mysql.com/doc/refman/8.0/en/information-schema.html)
- [Java 24 Virtual Threads](https://openjdk.org/jeps/444)

---

*本文档版本: v1.0 | 最后更新: 2024-12-08*