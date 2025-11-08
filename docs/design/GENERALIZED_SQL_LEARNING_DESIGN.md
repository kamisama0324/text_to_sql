# 泛化SQL模式学习系统设计

## 1. 设计目标

创建一个**数据库无关的、可泛化的SQL模式学习系统**，能够：
- 从任何数据库结构中学习通用模式
- 跨项目、跨数据库复用学习成果
- 通过抽象模式而非具体表名进行匹配
- 达到80%左右的准确率，但具备强泛化能力

## 2. 核心架构

```
用户查询 → 语义理解 → 模式匹配 → 动态适配 → SQL生成 → 用户反馈 → 模式学习
    ↑                                                               ↓
    ← ← ← ← ← ← ← ← ← 跨数据库模式库更新 ← ← ← ← ← ← ← ← ← ← ← ← ← ←
```

### 2.1 核心组件

- **SemanticAnalyzer**: 语义分析器 - 理解用户意图和实体语义
- **PatternMatcher**: 模式匹配器 - 匹配通用SQL模式
- **SchemaAdapter**: 模式适配器 - 将通用模式适配到具体数据库
- **GeneralizedLearner**: 泛化学习器 - 从反馈中学习通用规律

## 3. 数据结构设计

### 3.1 通用SQL模式

```java
@Data
@Builder
public class GeneralizedSqlPattern {
    private String patternId;
    private String patternName;
    
    // 意图语义而非具体关键词
    private List<String> intentSemantics;      // ["entity_association", "detail_query"]
    private List<String> entityTypes;          // ["user_like", "content_like", "transaction_like"]
    
    // 抽象SQL结构
    private SqlTemplate sqlTemplate;
    
    // 适用条件
    private List<String> applicableScenarios;
    private double generalConfidence;           // 泛化置信度
    
    // 使用统计
    private int crossDatabaseUsage;            // 跨数据库使用次数
    private Map<String, Integer> successByDbType; // 各数据库类型成功次数
}

@Data 
@Builder
public class SqlTemplate {
    // 使用占位符的SQL模板
    private String selectTemplate;    // "SELECT {main_fields}, {detail_fields}"
    private String fromTemplate;      // "FROM {main_entity}"
    private String joinTemplate;      // "LEFT JOIN {related_entity} ON {join_condition}"
    private String whereTemplate;     // "WHERE {main_entity}.{status_field} = 1"
    private String orderTemplate;     // "ORDER BY {main_entity}.{time_field} DESC"
    
    // 占位符映射规则
    private Map<String, PlaceholderRule> placeholderRules;
}
```

### 3.2 语义实体模式

```java
@Data
@Builder
public class EntitySemanticPattern {
    private String semanticType;        // "user_like", "product_like", "order_like"
    private List<String> namePatterns;  // ["user.*", "account.*", "profile.*"]
    private List<String> fieldPatterns; // [".*_id", "name", "email", "created_at"]
    private List<String> keywords;      // ["用户", "账号", "个人"]
    private double confidence;
}

@Data
@Builder
public class RelationshipSemanticPattern {
    private String relationType;        // "one_to_many", "many_to_many", "belongs_to"
    private String sourceEntityType;    // "user_like"
    private String targetEntityType;    // "profile_like" 
    private List<String> joinPatterns;  // ["{source}.id = {target}.{source_singular}_id"]
    private List<String> intentKeywords; // ["关联", "对应的", "相关的"]
    private double confidence;
}
```

### 3.3 函数语义模式

```java
@Data
@Builder
public class FunctionSemanticPattern {
    private String functionType;        // "aggregation", "string_operation", "date_operation"
    private String intentSemantic;      // "count_entity", "average_value", "filter_time"
    private List<String> triggerKeywords; // ["统计", "数量", "多少"]
    private String functionTemplate;    // "COUNT({entity_table}.*)"
    private List<String> contextRequirements; // ["requires_groupby", "requires_numeric_field"]
    private double confidence;
}
```

## 4. 核心服务实现

### 4.1 语义分析器

```java
@Service
@Slf4j
public class SemanticAnalyzer {
    
    private final EntitySemanticMatcher entityMatcher;
    private final IntentClassifier intentClassifier;
    
    /**
     * 分析用户查询的语义意图
     */
    public QuerySemantic analyzeQuery(String userQuery, List<Table> availableTables) {
        // 1. 识别查询意图
        QueryIntent intent = intentClassifier.classifyIntent(userQuery);
        
        // 2. 识别涉及的实体类型
        List<EntitySemantic> entities = identifyEntities(userQuery, availableTables);
        
        // 3. 识别关系类型
        List<RelationSemantic> relations = identifyRelations(userQuery, entities);
        
        // 4. 识别函数需求
        List<FunctionSemantic> functions = identifyFunctions(userQuery);
        
        return QuerySemantic.builder()
            .intent(intent)
            .entities(entities)
            .relations(relations)
            .functions(functions)
            .build();
    }
    
    /**
     * 基于语义相似度识别实体
     */
    private List<EntitySemantic> identifyEntities(String query, List<Table> tables) {
        List<EntitySemantic> entities = new ArrayList<>();
        
        for (Table table : tables) {
            EntitySemanticPattern pattern = findBestMatchingEntityPattern(table);
            if (pattern != null) {
                double relevance = calculateQueryRelevance(query, pattern.getKeywords());
                if (relevance > 0.3) {
                    entities.add(EntitySemantic.builder()
                        .tableName(table.getName())
                        .semanticType(pattern.getSemanticType())
                        .relevanceScore(relevance)
                        .build());
                }
            }
        }
        
        return entities.stream()
            .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
            .collect(Collectors.toList());
    }
    
    /**
     * 查找最匹配的实体模式
     */
    private EntitySemanticPattern findBestMatchingEntityPattern(Table table) {
        return entityPatterns.stream()
            .filter(pattern -> matchesTableStructure(table, pattern))
            .max(Comparator.comparing(EntitySemanticPattern::getConfidence))
            .orElse(null);
    }
    
    private boolean matchesTableStructure(Table table, EntitySemanticPattern pattern) {
        // 检查表名是否匹配模式
        boolean nameMatches = pattern.getNamePatterns().stream()
            .anyMatch(namePattern -> table.getName().matches(namePattern));
            
        // 检查字段是否匹配模式
        long matchingFields = pattern.getFieldPatterns().stream()
            .mapToLong(fieldPattern -> table.getColumns().stream()
                .mapToLong(col -> col.getName().matches(fieldPattern) ? 1 : 0)
                .sum())
            .sum();
            
        return nameMatches || matchingFields >= pattern.getFieldPatterns().size() * 0.6;
    }
}
```

### 4.2 通用模式匹配器

```java
@Service
@Slf4j
public class GeneralizedPatternMatcher {
    
    private final YamlPatternRepository patternRepository;
    
    /**
     * 基于语义匹配通用SQL模式
     */
    public List<GeneralizedSqlPattern> matchPatterns(QuerySemantic querySemantic) {
        List<GeneralizedSqlPattern> matchedPatterns = new ArrayList<>();
        
        // 1. 基于意图匹配
        List<GeneralizedSqlPattern> intentMatched = patternRepository.findByIntentSemantics(
            querySemantic.getIntent().getSemanticTypes()
        );
        
        // 2. 基于实体类型匹配
        List<GeneralizedSqlPattern> entityMatched = patternRepository.findByEntityTypes(
            querySemantic.getEntities().stream()
                .map(EntitySemantic::getSemanticType)
                .collect(Collectors.toList())
        );
        
        // 3. 计算匹配度
        Set<GeneralizedSqlPattern> candidates = new HashSet<>(intentMatched);
        candidates.addAll(entityMatched);
        
        for (GeneralizedSqlPattern pattern : candidates) {
            double matchScore = calculateMatchScore(pattern, querySemantic);
            if (matchScore > 0.4) {
                pattern.setCurrentMatchScore(matchScore);
                matchedPatterns.add(pattern);
            }
        }
        
        return matchedPatterns.stream()
            .sorted((a, b) -> Double.compare(b.getCurrentMatchScore(), a.getCurrentMatchScore()))
            .collect(Collectors.toList());
    }
    
    /**
     * 计算模式匹配度
     */
    private double calculateMatchScore(GeneralizedSqlPattern pattern, QuerySemantic semantic) {
        double intentScore = calculateIntentMatch(pattern.getIntentSemantics(), semantic.getIntent());
        double entityScore = calculateEntityMatch(pattern.getEntityTypes(), semantic.getEntities());
        double relationScore = calculateRelationMatch(pattern, semantic.getRelations());
        
        // 加权平均
        return intentScore * 0.4 + entityScore * 0.4 + relationScore * 0.2;
    }
}
```

### 4.3 动态Schema适配器

```java
@Service
@Slf4j
public class SchemaAdapter {
    
    /**
     * 将通用模式适配到具体数据库schema
     */
    public String adaptPatternToSchema(GeneralizedSqlPattern pattern, QuerySemantic semantic, 
                                     List<Table> availableTables) {
        SqlTemplate template = pattern.getSqlTemplate();
        
        // 1. 映射实体到具体表
        Map<String, String> entityTableMapping = mapEntitiesToTables(semantic.getEntities(), availableTables);
        
        // 2. 映射字段到具体列
        Map<String, List<String>> entityFieldMapping = mapEntityFields(entityTableMapping, availableTables);
        
        // 3. 生成JOIN条件
        List<String> joinConditions = generateJoinConditions(semantic.getRelations(), entityTableMapping, availableTables);
        
        // 4. 替换模板占位符
        String adaptedSql = replacePlaceholders(template, entityTableMapping, entityFieldMapping, joinConditions);
        
        return adaptedSql;
    }
    
    /**
     * 智能映射实体到表
     */
    private Map<String, String> mapEntitiesToTables(List<EntitySemantic> entities, List<Table> tables) {
        Map<String, String> mapping = new HashMap<>();
        
        for (EntitySemantic entity : entities) {
            // 基于语义类型和相似度找到最佳匹配表
            Table bestMatch = tables.stream()
                .filter(table -> calculateSemanticSimilarity(entity.getSemanticType(), table) > 0.5)
                .max(Comparator.comparing(table -> calculateSemanticSimilarity(entity.getSemanticType(), table)))
                .orElse(null);
                
            if (bestMatch != null) {
                mapping.put(entity.getSemanticType(), bestMatch.getName());
            }
        }
        
        return mapping;
    }
    
    /**
     * 智能生成JOIN条件
     */
    private List<String> generateJoinConditions(List<RelationSemantic> relations, 
                                               Map<String, String> tableMapping, 
                                               List<Table> tables) {
        List<String> joinConditions = new ArrayList<>();
        
        for (RelationSemantic relation : relations) {
            String sourceTable = tableMapping.get(relation.getSourceEntityType());
            String targetTable = tableMapping.get(relation.getTargetEntityType());
            
            if (sourceTable != null && targetTable != null) {
                String joinCondition = inferJoinCondition(sourceTable, targetTable, tables);
                if (joinCondition != null) {
                    joinConditions.add(joinCondition);
                }
            }
        }
        
        return joinConditions;
    }
    
    /**
     * 推断两表间的JOIN条件
     */
    private String inferJoinCondition(String sourceTable, String targetTable, List<Table> tables) {
        Table sourceTableObj = findTable(sourceTable, tables);
        Table targetTableObj = findTable(targetTable, tables);
        
        if (sourceTableObj == null || targetTableObj == null) {
            return null;
        }
        
        // 1. 检查是否有外键约束
        for (ForeignKey fk : targetTableObj.getForeignKeys()) {
            if (fk.getReferencedTableName().equals(sourceTable)) {
                return String.format("%s.%s = %s.%s", 
                    sourceTable, fk.getReferencedColumnName(),
                    targetTable, fk.getColumnName());
            }
        }
        
        // 2. 基于命名规则推断
        String sourceTableSingular = singularize(sourceTable);
        String expectedFkColumn = sourceTableSingular + "_id";
        
        boolean targetHasFk = targetTableObj.getColumns().stream()
            .anyMatch(col -> col.getName().equals(expectedFkColumn));
            
        if (targetHasFk) {
            return String.format("%s.id = %s.%s", sourceTable, targetTable, expectedFkColumn);
        }
        
        // 3. 其他推断规则...
        return null;
    }
}
```

### 4.4 泛化学习器

```java
@Service
@Slf4j
public class GeneralizedLearner {
    
    private final YamlPatternRepository patternRepository;
    
    /**
     * 从用户反馈中学习泛化模式
     */
    public void learnFromFeedback(String userQuery, String originalSql, String correctedSql, 
                                 QuerySemantic semantic, List<Table> schemaInfo) {
        // 1. 分析修正前后的差异
        SqlDifference diff = analyzeSqlDifference(originalSql, correctedSql);
        
        // 2. 提取泛化模式
        GeneralizedSqlPattern newPattern = extractGeneralizedPattern(userQuery, correctedSql, semantic, diff);
        
        // 3. 更新或创建模式
        updateOrCreatePattern(newPattern);
        
        // 4. 学习新的语义关联
        learnSemanticAssociations(semantic, correctedSql, schemaInfo);
        
        log.info("从用户反馈中学习到新的泛化模式: {}", newPattern.getPatternName());
    }
    
    /**
     * 提取泛化的SQL模式
     */
    private GeneralizedSqlPattern extractGeneralizedPattern(String userQuery, String sql, 
                                                           QuerySemantic semantic, SqlDifference diff) {
        // 1. 将具体表名抽象为语义类型
        String abstractedSql = abstractTableNames(sql, semantic);
        
        // 2. 将具体字段抽象为语义字段
        abstractedSql = abstractFieldNames(abstractedSql, semantic);
        
        // 3. 提取SQL模板结构
        SqlTemplate template = extractSqlTemplate(abstractedSql);
        
        // 4. 识别适用场景
        List<String> scenarios = identifyApplicableScenarios(userQuery, semantic);
        
        return GeneralizedSqlPattern.builder()
            .patternId(generatePatternId())
            .patternName(generatePatternName(semantic))
            .intentSemantics(extractIntentSemantics(userQuery))
            .entityTypes(extractEntityTypes(semantic))
            .sqlTemplate(template)
            .applicableScenarios(scenarios)
            .generalConfidence(0.7) // 初始置信度
            .crossDatabaseUsage(1)
            .build();
    }
    
    /**
     * 将具体表名抽象为语义占位符
     */
    private String abstractTableNames(String sql, QuerySemantic semantic) {
        String abstractedSql = sql;
        
        for (EntitySemantic entity : semantic.getEntities()) {
            String placeholder = "{" + entity.getSemanticType() + "}";
            abstractedSql = abstractedSql.replaceAll("\\b" + entity.getTableName() + "\\b", placeholder);
        }
        
        return abstractedSql;
    }
    
    /**
     * 学习语义关联模式
     */
    private void learnSemanticAssociations(QuerySemantic semantic, String correctSql, List<Table> schemaInfo) {
        // 1. 学习实体识别模式
        for (EntitySemantic entity : semantic.getEntities()) {
            Table table = findTableByName(entity.getTableName(), schemaInfo);
            if (table != null) {
                learnEntityPattern(entity.getSemanticType(), table);
            }
        }
        
        // 2. 学习关系识别模式
        List<JoinInfo> joins = extractJoinInfo(correctSql);
        for (JoinInfo join : joins) {
            learnRelationshipPattern(join, semantic);
        }
    }
    
    /**
     * 跨数据库验证模式有效性
     */
    public void validatePatternAcrossDatabases(GeneralizedSqlPattern pattern) {
        // 这是一个后台任务，在不同类型的数据库上测试模式的适用性
        // 根据验证结果调整模式的泛化置信度
    }
}
```

## 5. 配置文件结构

### 5.1 泛化模式配置 (`generalized_patterns.yml`)

```yaml
# 通用SQL模式库
generalized_sql_patterns:
  - pattern_id: "entity_detail_association"
    pattern_name: "实体详情关联查询"
    intent_semantics: ["entity_association", "detail_query"]
    entity_types: ["primary_entity", "detail_entity"]
    sql_template:
      select_template: "SELECT {primary_entity_fields}, {detail_entity_fields}"
      from_template: "FROM {primary_entity}"
      join_template: "LEFT JOIN {detail_entity} ON {primary_entity}.id = {detail_entity}.{primary_entity_singular}_id"
      where_template: "WHERE {primary_entity}.{status_field} = 1"
      order_template: "ORDER BY {primary_entity}.{time_field} DESC"
    placeholder_rules:
      primary_entity_fields: 
        rule: "select_main_fields"
        fallback: "*"
      detail_entity_fields:
        rule: "select_detail_fields" 
        fallback: "*"
      status_field:
        patterns: ["is_valid", "status", "is_active", "enabled"]
        fallback: "1=1"
      time_field:
        patterns: ["created_at", "updated_at", "time", "date"]
        fallback: "id"
    applicable_scenarios: ["user_profile_query", "product_detail_query", "order_item_query"]
    general_confidence: 0.85
    cross_database_usage: 45
    success_by_db_type:
      mysql: 38
      postgresql: 35
      sqlite: 28

  - pattern_id: "count_with_grouping"
    pattern_name: "分组统计查询"
    intent_semantics: ["count_statistics", "group_analysis"]
    entity_types: ["countable_entity"]
    sql_template:
      select_template: "SELECT {group_field}, COUNT(*) as count"
      from_template: "FROM {countable_entity}"
      where_template: "WHERE {filter_conditions}"
      group_template: "GROUP BY {group_field}"
      order_template: "ORDER BY count DESC"
    applicable_scenarios: ["statistics_by_category", "user_analysis", "sales_analysis"]
    general_confidence: 0.92
    cross_database_usage: 62
    
  - pattern_id: "time_range_filter"
    pattern_name: "时间范围筛选"
    intent_semantics: ["time_filter", "range_query"] 
    entity_types: ["time_aware_entity"]
    sql_template:
      select_template: "SELECT {entity_fields}"
      from_template: "FROM {time_aware_entity}"
      where_template: "WHERE {time_field} {time_operator} {time_value}"
      order_template: "ORDER BY {time_field} DESC"
    placeholder_rules:
      time_operator:
        today: "= CURDATE()"
        yesterday: "= DATE_SUB(CURDATE(), INTERVAL 1 DAY)"
        this_week: ">= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)"
        this_month: ">= DATE_FORMAT(CURDATE(), '%Y-%m-01')"
        this_year: ">= DATE_FORMAT(CURDATE(), '%Y-01-01')"
    general_confidence: 0.88
    cross_database_usage: 53
```

### 5.2 语义实体模式 (`entity_semantic_patterns.yml`)

```yaml
# 实体语义模式
entity_semantic_patterns:
  - semantic_type: "user_like"
    name_patterns: 
      - ".*user.*"
      - ".*account.*" 
      - ".*profile.*"
      - ".*member.*"
      - ".*customer.*"
    field_patterns:
      - ".*id"
      - ".*name.*"
      - ".*email.*" 
      - ".*phone.*"
      - "created_at"
      - "updated_at"
    keywords: ["用户", "账号", "个人", "会员", "客户"]
    confidence: 0.9
    
  - semantic_type: "content_like"
    name_patterns:
      - ".*post.*"
      - ".*article.*"
      - ".*content.*"
      - ".*news.*"
      - ".*blog.*"
    field_patterns:
      - ".*id"
      - "title"
      - ".*content.*"
      - ".*body.*"
      - "author_id"
      - "created_at"
    keywords: ["文章", "内容", "帖子", "新闻", "博客"]
    confidence: 0.85
    
  - semantic_type: "transaction_like" 
    name_patterns:
      - ".*order.*"
      - ".*transaction.*"
      - ".*payment.*"
      - ".*purchase.*"
    field_patterns:
      - ".*id"
      - "amount"
      - ".*price.*"
      - ".*total.*"
      - "user_id"
      - "created_at"
    keywords: ["订单", "交易", "支付", "购买", "金额"]
    confidence: 0.87

# 关系语义模式
relationship_semantic_patterns:
  - relation_type: "one_to_many_detail"
    source_entity_type: "primary_entity"
    target_entity_type: "detail_entity"
    join_patterns: 
      - "{source}.id = {target}.{source_singular}_id"
      - "{source}.{source}_id = {target}.id"
    intent_keywords: ["关联", "对应的", "相关的", "详细", "明细"]
    confidence: 0.82
    
  - relation_type: "user_ownership"
    source_entity_type: "user_like"
    target_entity_type: "*"
    join_patterns:
      - "{source}.id = {target}.user_id"
      - "{source}.id = {target}.owner_id"
      - "{source}.id = {target}.created_by"
    intent_keywords: ["我的", "用户的", "个人的", "拥有的"]
    confidence: 0.88

# 函数语义模式
function_semantic_patterns:
  aggregation:
    - function_type: "count_all"
      intent_semantic: "count_entity"
      trigger_keywords: ["统计", "数量", "多少个", "总数", "计数"]
      function_template: "COUNT(*)"
      context_requirements: []
      confidence: 0.95
      
    - function_type: "average"
      intent_semantic: "average_value"
      trigger_keywords: ["平均", "均值", "平均数"]
      function_template: "AVG({numeric_field})"
      context_requirements: ["requires_numeric_field"]
      confidence: 0.88
      
    - function_type: "sum_total"
      intent_semantic: "sum_value"
      trigger_keywords: ["总计", "合计", "总和", "求和"]
      function_template: "SUM({numeric_field})"
      context_requirements: ["requires_numeric_field"]
      confidence: 0.91
      
  string_operations:
    - function_type: "contains_match"
      intent_semantic: "text_contains"
      trigger_keywords: ["包含", "含有", "包括"]
      function_template: "{text_field} LIKE '%{keyword}%'"
      context_requirements: ["requires_text_field"]
      confidence: 0.89
      
    - function_type: "starts_with"
      intent_semantic: "text_starts"
      trigger_keywords: ["以...开头", "开始于"]
      function_template: "{text_field} LIKE '{keyword}%'"
      context_requirements: ["requires_text_field"] 
      confidence: 0.84
      
  time_operations:
    - function_type: "today_filter"
      intent_semantic: "today_records"
      trigger_keywords: ["今天", "今日", "当天"]
      function_template: "DATE({date_field}) = CURDATE()"
      context_requirements: ["requires_date_field"]
      confidence: 0.93
      
    - function_type: "recent_filter"
      intent_semantic: "recent_records"
      trigger_keywords: ["最近", "近期", "最新"]
      function_template: "{date_field} >= DATE_SUB(NOW(), INTERVAL 7 DAY)"
      context_requirements: ["requires_date_field"]
      confidence: 0.87
```

## 6. 实施优势

### 6.1 泛化能力
- **跨数据库适用**：模式不依赖具体表名，可在不同项目间复用
- **语义理解**：基于业务语义而非字面匹配，理解能力更强
- **动态适配**：自动将抽象模式映射到具体数据库结构

### 6.2 学习能力
- **持续改进**：从每次反馈中学习新的泛化规律
- **模式复用**：一次学习，多处受益
- **置信度管理**：基于跨数据库使用效果调整模式可信度

### 6.3 实际效果预期
- **准确率**：预期达到75-85%的准确率
- **覆盖率**：能处理80%以上的常见查询场景
- **泛化性**：学习成果可跨项目复用，降低整体训练成本

## 7. 实施计划

### 阶段1：语义理解基础 (2周)
- [ ] 实现SemanticAnalyzer语义分析器
- [ ] 创建基础的语义模式配置文件
- [ ] 实现实体和关系语义识别

### 阶段2：模式匹配引擎 (2周) 
- [ ] 实现GeneralizedPatternMatcher
- [ ] 实现SchemaAdapter动态适配器
- [ ] 集成到现有SQL生成流程

### 阶段3：学习反馈机制 (1-2周)
- [ ] 实现GeneralizedLearner泛化学习器
- [ ] 实现用户反馈收集和处理
- [ ] 实现模式自动更新机制

### 阶段4：优化和验证 (1周)
- [ ] 跨数据库验证测试
- [ ] 性能优化
- [ ] 文档完善

这个泛化设计能够实现真正的跨数据库学习，学习成果可以在不同项目间复用，大大提高了系统的实用价值。