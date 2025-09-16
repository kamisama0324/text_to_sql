# SpringAI-MCP 核心流程详解

## 1. Text2SQL 核心转换流程

### 1.1 完整处理流程

```mermaid
graph TD
    A[用户输入自然语言查询] --> B{输入验证}
    B -->|有效| C[Text2SqlController接收]
    B -->|无效| Z1[返回错误信息]
    
    C --> D[Text2SqlService.convertToSql]
    D --> E[获取目标数据库名称]
    E --> F[SchemaCache.getSchema]
    
    F --> G{缓存检查}
    G -->|命中| H[返回缓存的数据库结构]
    G -->|未命中| I[SchemaDiscoveryService.discoverSchema]
    
    I --> J[查询INFORMATION_SCHEMA]
    J --> K[分析表结构和字段]
    K --> L[推断外键关系]
    L --> M[更新本地缓存]
    M --> H
    
    H --> N[生成AI上下文结构描述]
    N --> O[构建结构化提示词]
    O --> P[调用DeepSeek API]
    
    P --> Q{AI响应检查}
    Q -->|成功| R[清理生成的SQL]
    Q -->|失败| Z2[返回AI错误]
    
    R --> S[安全验证SQL]
    S --> T{安全检查}
    T -->|通过| U[生成SQL解释]
    T -->|失败| Z3[返回安全错误]
    
    U --> V[Text2SqlController返回结果]
    V --> W[前端显示结果]
```

### 1.2 数据库结构发现详细流程

```mermaid
sequenceDiagram
    participant C as SchemaCache
    participant S as SchemaDiscoveryService
    participant DB as MySQL Database
    participant Cache as LocalCache
    
    Note over C,Cache: 数据库结构发现流程
    
    C->>C: 检查本地缓存
    alt 缓存存在且未过期
        C-->>C: 返回缓存数据
    else 需要重新发现
        C->>S: discoverSchema(database)
        
        S->>DB: SELECT * FROM INFORMATION_SCHEMA.TABLES
        DB-->>S: 返回表信息
        
        S->>DB: SELECT * FROM INFORMATION_SCHEMA.COLUMNS
        DB-->>S: 返回字段信息
        
        S->>DB: SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
        DB-->>S: 返回主外键信息
        
        S->>S: 分析字段类型和约束
        S->>S: 推断隐含的外键关系
        S->>S: 构建DatabaseSchema对象
        
        S-->>C: 返回完整数据库结构
        C->>Cache: 更新本地缓存(TTL: 3600s)
    end
```

### 1.3 SQL安全验证流程

```mermaid
flowchart TD
    A[生成的原始SQL] --> B[cleanGeneratedSql]
    B --> C[移除markdown标记]
    C --> D[清理空白字符]
    D --> E[确保分号结尾]
    
    E --> F[validateGeneratedSql]
    F --> G{以SELECT开头?}
    G -->|否| H[抛出非法操作异常]
    
    G -->|是| I[检查危险关键字]
    I --> J{包含DELETE/UPDATE/DROP等?}
    J -->|是| K[抛出危险操作异常]
    
    J -->|否| L[检查禁用的SQL类型]
    L --> M{以WITH/SHOW/DESCRIBE开头?}
    M -->|是| N[抛出禁用类型异常]
    
    M -->|否| O[检查SQL长度]
    O --> P{长度>3000字符?}
    P -->|是| Q[抛出过长异常]
    
    P -->|否| R[检查多语句]
    R --> S{包含多个语句?}
    S -->|是| T[抛出多语句异常]
    
    S -->|否| U[验证通过]
    U --> V[执行SQL查询]
    
    style U fill:#90EE90
    style H fill:#FFB6C1
    style K fill:#FFB6C1
    style N fill:#FFB6C1
    style Q fill:#FFB6C1
    style T fill:#FFB6C1
```

## 2. 缓存系统流程

### 2.1 多级缓存架构

```mermaid
graph TB
    subgraph "应用层"
        Service[Text2SqlService]
    end
    
    subgraph "缓存层"
        SchemaCache[SchemaCache Service]
        
        subgraph "本地缓存"
            LocalMap[ConcurrentHashMap]
            TTL[TTL管理机制]
            LocalMap --> TTL
        end
        
        subgraph "Spring缓存抽象"
            CacheManager[CacheManager]
            CacheAnn[@Cacheable注解]
            CacheManager --> CacheAnn
        end
        
        subgraph "分布式缓存 (可选)"
            Redis[(Redis)]
            RedisTemplate[RedisTemplate]
            Redis --> RedisTemplate
        end
    end
    
    subgraph "数据源"
        Database[(MySQL)]
    end
    
    Service --> SchemaCache
    SchemaCache --> LocalMap
    SchemaCache --> CacheManager
    CacheManager -.-> RedisTemplate
    SchemaCache --> Database
    
    style LocalMap fill:#90EE90
    style Redis fill:#87CEEB,stroke-dasharray: 5 5
```

### 2.2 缓存更新策略

```mermaid
stateDiagram-v2
    [*] --> CacheEmpty : 首次访问
    CacheEmpty --> LoadingFromDB : 开始加载数据库结构
    LoadingFromDB --> CacheFresh : 加载完成，缓存新鲜
    
    CacheFresh --> CacheValid : 访问时检查TTL
    CacheValid --> CacheFresh : TTL未过期
    CacheValid --> CacheExpired : TTL已过期
    
    CacheExpired --> LoadingFromDB : 重新加载
    
    CacheFresh --> CacheInvalidated : 手动清除缓存
    CacheInvalidated --> CacheEmpty : 缓存已清空
    
    note right of CacheFresh : TTL: 3600秒<br/>自动过期策略
    note right of LoadingFromDB : 数据库结构发现<br/>大约需要5-7秒
```

## 3. MCP 集成流程

### 3.1 MCP 服务架构

```mermaid
graph TB
    subgraph "MCP客户端层"
        McpController[MCP Controller]
        McpService[MCP Service]
    end
    
    subgraph "并发处理层"
        VirtualThreads[Virtual Threads<br/>Java 24]
        StructuredConcurrency[Structured Concurrency<br/>StructuredTaskScope]
    end
    
    subgraph "MCP服务器"
        FilesystemMCP[Filesystem MCP Server]
        GithubMCP[GitHub MCP Server]
    end
    
    subgraph "任务执行"
        FilesystemTask[文件系统任务]
        GithubTask[GitHub任务]
        MultiTask[多任务并行执行]
    end
    
    McpController --> McpService
    McpService --> VirtualThreads
    VirtualThreads --> StructuredConcurrency
    
    StructuredConcurrency --> FilesystemMCP
    StructuredConcurrency --> GithubMCP
    
    FilesystemMCP --> FilesystemTask
    GithubMCP --> GithubTask
    StructuredConcurrency --> MultiTask
```

### 3.2 结构化并发执行流程

```mermaid
sequenceDiagram
    participant Client as MCP Client
    participant Service as MCP Service
    participant Scope as StructuredTaskScope
    participant FS as Filesystem Task
    participant GH as GitHub Task
    
    Client->>Service: executeMultipleTasks(fsPrompt, ghPrompt)
    Service->>Scope: new StructuredTaskScope.ShutdownOnFailure()
    
    par 并行执行任务
        Service->>FS: scope.fork(() -> filesystemTask)
    and
        Service->>GH: scope.fork(() -> githubTask)
    end
    
    Note over FS,GH: 虚拟线程并行执行
    
    FS-->>Scope: 任务结果 (虚拟线程: true)
    GH-->>Scope: 任务结果 (虚拟线程: true)
    
    Service->>Scope: scope.join()
    Service->>Scope: scope.throwIfFailed()
    
    Service->>Service: 收集所有任务结果
    Service-->>Client: 返回合并结果
    
    Note over Service,Client: 所有任务完成或失败时返回
```

## 4. 前端交互流程

### 4.1 用户界面交互流程

```mermaid
stateDiagram-v2
    [*] --> PageLoad : 页面加载
    PageLoad --> CheckConnection : 检查数据库连接
    CheckConnection --> LoadSchema : 连接成功，加载数据库结构
    LoadSchema --> ReadyForQuery : 准备接收查询
    
    ReadyForQuery --> QueryInput : 用户输入查询
    QueryInput --> Validating : 前端验证输入
    Validating --> SendingRequest : 发送API请求
    
    SendingRequest --> ProcessingQuery : 后端处理中
    ProcessingQuery --> ShowResults : 显示结果
    ProcessingQuery --> ShowError : 显示错误信息
    
    ShowResults --> ReadyForQuery : 准备下一次查询
    ShowError --> ReadyForQuery : 修正后重新查询
    
    ReadyForQuery --> ExecuteSQL : 直接执行SQL
    ExecuteSQL --> ShowQueryResults : 显示查询结果
    ShowQueryResults --> ReadyForQuery
    
    note right of ProcessingQuery : 显示加载状态<br/>约10-15秒处理时间
    note right of LoadSchema : 显示数据库表结构<br/>支持折叠展开
```

### 4.2 前端状态管理

```mermaid
graph TD
    subgraph "Vue组件状态"
        ConnectionStatus[connectionStatus<br/>数据库连接状态]
        Schema[schema<br/>数据库结构]
        UserQuery[userQuery<br/>用户输入]
        Results[queryResults<br/>查询结果]
        Loading[各种loading状态]
    end
    
    subgraph "API交互"
        CheckConn[检查连接API]
        LoadSchemaAPI[加载结构API]
        ConvertAPI[转换SQL API]
        ExecuteAPI[执行SQL API]
    end
    
    subgraph "UI组件"
        NavBar[导航栏]
        SidePanel[侧边数据库面板]
        QueryPanel[查询输入面板]
        ResultPanel[结果显示面板]
    end
    
    ConnectionStatus --> NavBar
    Schema --> SidePanel
    UserQuery --> QueryPanel
    Results --> ResultPanel
    
    CheckConn --> ConnectionStatus
    LoadSchemaAPI --> Schema
    ConvertAPI --> Results
    ExecuteAPI --> Results
    
    Loading --> NavBar
    Loading --> SidePanel
    Loading --> QueryPanel
```

## 5. 学习系统流程

### 5.1 泛化学习机制

```mermaid
graph TB
    subgraph "输入分析"
        UserQuery[用户查询]
        QueryAnalysis[查询分析]
        IntentRecognition[意图识别]
    end
    
    subgraph "模式匹配"
        PatternMatcher[模式匹配器]
        EntityRecognition[实体识别]
        RelationshipAnalysis[关系分析]
    end
    
    subgraph "学习数据"
        YamlConfig[generalized-patterns.yml]
        EntityPatterns[实体语义模式]
        SQLPatterns[SQL模式]
        RelationPatterns[关系模式]
    end
    
    subgraph "反馈学习"
        UserFeedback[用户反馈]
        PatternUpdate[模式更新]
        ConfidenceAdjust[置信度调整]
    end
    
    UserQuery --> QueryAnalysis
    QueryAnalysis --> IntentRecognition
    IntentRecognition --> PatternMatcher
    
    PatternMatcher --> EntityRecognition
    EntityRecognition --> RelationshipAnalysis
    
    YamlConfig --> EntityPatterns
    YamlConfig --> SQLPatterns
    YamlConfig --> RelationPatterns
    
    EntityPatterns --> PatternMatcher
    SQLPatterns --> PatternMatcher
    RelationPatterns --> PatternMatcher
    
    UserFeedback --> PatternUpdate
    PatternUpdate --> ConfidenceAdjust
    ConfidenceAdjust --> YamlConfig
```

### 5.2 模式学习生命周期

```mermaid
sequenceDiagram
    participant User as 用户
    participant Service as GeneralizedLearner
    participant Config as YAML配置
    participant Pattern as 模式匹配器
    
    Note over User,Pattern: 初始学习阶段
    
    User->>Service: 提供查询和反馈
    Service->>Pattern: 分析查询模式
    Pattern->>Pattern: 提取语义特征
    Pattern->>Config: 更新模式配置
    
    Note over User,Pattern: 应用学习阶段
    
    User->>Service: 新的查询请求
    Service->>Pattern: 匹配已学习模式
    Pattern->>Config: 查询匹配模式
    Config-->>Pattern: 返回匹配结果
    Pattern-->>Service: 应用学习模式
    Service-->>User: 优化的SQL生成
    
    Note over User,Pattern: 反馈优化阶段
    
    User->>Service: 提供正/负反馈
    Service->>Config: 调整模式置信度
    Config->>Config: 更新学习历史
    Config-->>Service: 确认更新完成
```

## 6. 错误处理和恢复流程

### 6.1 异常处理层次

```mermaid
graph TD
    subgraph "应用层异常"
        UserInputError[用户输入错误]
        ValidationError[数据验证错误]
        BusinessLogicError[业务逻辑错误]
    end
    
    subgraph "集成层异常"
        AIServiceError[AI服务异常]
        DatabaseError[数据库连接异常]
        CacheError[缓存服务异常]
    end
    
    subgraph "系统层异常"
        NetworkError[网络异常]
        TimeoutError[超时异常]
        ResourceError[资源不足异常]
    end
    
    subgraph "处理策略"
        Retry[重试机制]
        Fallback[降级处理]
        ErrorResponse[错误响应]
        Logging[日志记录]
    end
    
    UserInputError --> ErrorResponse
    ValidationError --> ErrorResponse
    BusinessLogicError --> ErrorResponse
    
    AIServiceError --> Retry
    DatabaseError --> Fallback
    CacheError --> Fallback
    
    NetworkError --> Retry
    TimeoutError --> Retry
    ResourceError --> Logging
    
    Retry --> ErrorResponse
    Fallback --> ErrorResponse
    
    style Retry fill:#FFE4B5
    style Fallback fill:#E6E6FA
    style ErrorResponse fill:#FFB6C1
```

### 6.2 服务降级策略

```mermaid
stateDiagram-v2
    [*] --> FullService : 完整服务状态
    FullService --> AIServiceDown : AI服务不可用
    FullService --> DatabaseDown : 数据库不可用
    FullService --> CacheDown : 缓存服务不可用
    
    AIServiceDown --> ErrorResponse : 返回服务不可用错误
    DatabaseDown --> CachedResponse : 使用缓存数据响应
    CacheDown --> DirectDatabase : 直接查询数据库
    
    AIServiceDown --> [*] : AI服务恢复
    DatabaseDown --> [*] : 数据库恢复
    CacheDown --> [*] : 缓存服务恢复
    
    CachedResponse --> [*] : 正常响应
    DirectDatabase --> [*] : 正常响应
    ErrorResponse --> [*] : 错误响应
    
    note right of ErrorResponse : 友好的错误信息<br/>建议用户稍后重试
    note right of CachedResponse : 使用历史缓存数据<br/>标记为降级服务
    note right of DirectDatabase : 跳过缓存层<br/>直接访问数据库
```

---

*最后更新: 2025-09-11*