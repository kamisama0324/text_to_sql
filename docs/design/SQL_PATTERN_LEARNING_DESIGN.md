# SQL模式学习系统设计方案

## 1. 系统概述

本系统通过用户反馈和配置化的方式，实现SQL生成的持续学习和优化。不依赖复杂的模型训练，而是通过配置文件和规则引擎来积累知识，逐步提高SQL生成的准确性。

## 2. 核心架构

```
用户反馈 → 模式提取 → 配置更新 → 规则应用 → SQL生成优化
    ↑                                              ↓
    ← ← ← ← ← ← 效果验证 ← ← ← ← ← ← ← ← ← ← ← ← ←
```

### 2.1 主要组件

- **SqlPatternLearningService**: SQL模式学习和提取服务
- **FeedbackLearningService**: 用户反馈处理和学习服务  
- **YamlConfigManager**: YAML配置文件管理器
- **PatternMatcher**: 模式匹配和应用引擎
- **ConfigAutoUpdater**: 配置自动更新机制

## 3. 数据结构设计

### 3.1 SQL模式实体

```java
@Data
@Builder
public class SqlPattern {
    private String id;
    private List<String> intentKeywords;      // 意图关键词
    private String tablePattern;             // 表模式
    private String joinPattern;              // JOIN模式
    private String functionPattern;          // 函数模式
    private String wherePattern;             // WHERE条件模式
    private String sqlTemplate;             // SQL模板
    private double confidence;               // 置信度
    private int usageCount;                  // 使用次数
    private int successCount;                // 成功次数
    private int failureCount;                // 失败次数
    private LocalDateTime lastUsed;          // 最后使用时间
    private LocalDateTime createdAt;         // 创建时间
}

@Data
@Builder
public class RelationshipRule {
    private String sourceTable;
    private String sourceColumn;
    private String targetTable;
    private String targetColumn;
    private String joinCondition;
    private double confidence;
    private int usageCount;
    private LocalDateTime lastUsed;
    private String learnedFrom;              // 学习来源
}

@Data
@Builder
public class FunctionPattern {
    private List<String> intentKeywords;
    private String functionTemplate;
    private String description;
    private List<String> examples;
    private double confidence;
    private int usageCount;
}
```

### 3.2 配置文件结构

**relationship_rules.yml**
```yaml
# 表关联规则配置
table_relationships:
  echoing_characters:
    - target_table: echoing_third_accounts
      source_column: id
      target_column: character_id
      join_condition: "ec.id = eta.character_id"
      confidence: 0.95
      usage_count: 15
      last_used: "2025-09-10T10:30:00"
      learned_from: "user_feedback"
      
  user_profiles:
    - target_table: user_accounts
      source_column: user_id
      target_column: id
      join_condition: "up.user_id = ua.id"
      confidence: 0.88
      usage_count: 5
      learned_from: "naming_pattern"

# 命名模式推断规则      
naming_patterns:
  foreign_key_patterns:
    - pattern: "{table_singular}_id"
      target_table_pattern: "{table_singular}s"
      target_column: "id"
      confidence: 0.85
      examples: 
        - "user_id -> users.id"
        - "category_id -> categories.id"
        
    - pattern: "{prefix}_id" 
      target_table_pattern: "{prefix}s"
      target_column: "id"
      confidence: 0.75
      
  special_mappings:
    - source_pattern: "character_id"
      target_table: "echoing_characters"
      target_column: "id"
      confidence: 0.98
      reason: "用户反馈确认"

# 表名变形规则
table_name_transformations:
  - singular: "category"
    plural: "categories"
  - singular: "company"
    plural: "companies"
  - singular: "person"
    plural: "people"
  - singular: "child"
    plural: "children"
```

**query_patterns.yml**
```yaml
# 查询意图模式
query_intentions:
  - id: "user_character_association"
    keywords: ["查询", "人设", "关联", "账号", "人设信息", "账号信息"]
    tables: ["echoing_characters", "echoing_third_accounts"]
    sql_template: |
      SELECT {character_fields}, {account_fields}
      FROM echoing_characters ec
      LEFT JOIN echoing_third_accounts eta ON ec.id = eta.character_id
      WHERE ec.is_valid = 1
      {additional_conditions}
      LIMIT {limit}
    confidence: 0.92
    usage_count: 8
    success_count: 7
    failure_count: 1
    last_used: "2025-09-10T15:20:00"
    
  - id: "count_statistics"
    keywords: ["统计", "数量", "总数", "多少个", "计算"]
    sql_template: |
      SELECT COUNT(*) as total_count
      FROM {main_table}
      WHERE {conditions}
    functions: ["COUNT"]
    confidence: 0.89
    usage_count: 12
    
  - id: "group_statistics"
    keywords: ["按照", "分组", "统计", "各个", "每个"]
    sql_template: |
      SELECT {group_field}, COUNT(*) as count
      FROM {main_table}
      WHERE {conditions}
      GROUP BY {group_field}
      ORDER BY count DESC
    functions: ["COUNT", "GROUP BY"]
    confidence: 0.85
    usage_count: 6

# 函数使用模式
function_patterns:
  aggregation:
    - intent_keywords: ["统计", "总数", "数量", "多少个"]
      function_template: "COUNT(*)"
      description: "统计记录总数"
      examples: 
        - "统计用户数量 -> SELECT COUNT(*) FROM users"
        - "有多少个订单 -> SELECT COUNT(*) FROM orders"
      confidence: 0.95
      usage_count: 25
      
    - intent_keywords: ["平均", "平均值", "均值"]
      function_template: "AVG({field})"
      description: "计算平均值"
      examples:
        - "平均年龄 -> SELECT AVG(age) FROM users"
        - "平均价格 -> SELECT AVG(price) FROM products"
      confidence: 0.88
      usage_count: 8
      
    - intent_keywords: ["最大", "最高", "最大值"]
      function_template: "MAX({field})"
      description: "获取最大值"
      confidence: 0.90
      usage_count: 15
      
    - intent_keywords: ["最小", "最低", "最小值"]  
      function_template: "MIN({field})"
      description: "获取最小值"
      confidence: 0.90
      usage_count: 10

  string_functions:
    - intent_keywords: ["包含", "含有", "包括"]
      function_template: "LIKE '%{keyword}%'"
      description: "模糊匹配包含关键词"
      examples:
        - "名字包含张 -> WHERE name LIKE '%张%'"
      confidence: 0.92
      usage_count: 18
      
    - intent_keywords: ["以...开头", "开始于"]
      function_template: "LIKE '{keyword}%'"
      description: "匹配开头"
      confidence: 0.85
      usage_count: 7
      
    - intent_keywords: ["以...结尾", "结束于"]
      function_template: "LIKE '%{keyword}'"
      description: "匹配结尾"  
      confidence: 0.83
      usage_count: 5

  date_functions:
    - intent_keywords: ["今天", "今日", "当天"]
      function_template: "DATE({date_field}) = CURDATE()"
      description: "筛选今天的记录"
      examples:
        - "今天注册的用户 -> WHERE DATE(created_at) = CURDATE()"
      confidence: 0.94
      usage_count: 22
      
    - intent_keywords: ["昨天", "昨日"]
      function_template: "DATE({date_field}) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)"
      description: "筛选昨天的记录"
      confidence: 0.88
      usage_count: 8
      
    - intent_keywords: ["本周", "这周", "本星期"]
      function_template: "YEARWEEK({date_field}) = YEARWEEK(NOW())"
      description: "筛选本周的记录"
      confidence: 0.82
      usage_count: 6
      
    - intent_keywords: ["本月", "这个月", "当月"]
      function_template: "YEAR({date_field}) = YEAR(NOW()) AND MONTH({date_field}) = MONTH(NOW())"
      description: "筛选本月的记录"
      confidence: 0.85
      usage_count: 14
      
    - intent_keywords: ["今年", "本年", "当年"]
      function_template: "YEAR({date_field}) = YEAR(NOW())"
      description: "筛选今年的记录"
      confidence: 0.87
      usage_count: 9

  comparison_functions:
    - intent_keywords: ["大于", ">", "超过", "高于"]
      function_template: "{field} > {value}"
      description: "大于比较"
      confidence: 0.91
      usage_count: 16
      
    - intent_keywords: ["小于", "<", "低于", "少于"]
      function_template: "{field} < {value}"
      description: "小于比较"
      confidence: 0.91
      usage_count: 14
      
    - intent_keywords: ["等于", "=", "是", "为"]
      function_template: "{field} = '{value}'"
      description: "等于比较"
      confidence: 0.95
      usage_count: 35
      
    - intent_keywords: ["不等于", "!=", "<>", "不是"]
      function_template: "{field} != '{value}'"
      description: "不等于比较"
      confidence: 0.89
      usage_count: 11
      
    - intent_keywords: ["介于", "之间", "范围"]
      function_template: "{field} BETWEEN {min_value} AND {max_value}"
      description: "范围查询"
      confidence: 0.84
      usage_count: 7

# 常见查询条件模式
condition_patterns:
  - intent_keywords: ["有效的", "启用的", "可用的"]
    condition_template: "is_valid = 1"
    description: "筛选有效记录"
    confidence: 0.88
    usage_count: 19
    
  - intent_keywords: ["删除的", "无效的", "禁用的"]
    condition_template: "is_deleted = 1"
    description: "筛选已删除记录"
    confidence: 0.85
    usage_count: 8
    
  - intent_keywords: ["最新的", "最近的"]
    condition_template: "ORDER BY created_at DESC"
    description: "按创建时间倒序"
    confidence: 0.92
    usage_count: 24
    
  - intent_keywords: ["前N个", "前{num}个", "最多{num}个"]
    condition_template: "LIMIT {num}"
    description: "限制结果数量"
    confidence: 0.94
    usage_count: 31
```

## 4. 核心服务实现

### 4.1 SQL模式学习服务

```java
@Service
@Slf4j
public class SqlPatternLearningService {
    
    private final YamlConfigManager configManager;
    private final PatternExtractor patternExtractor;
    
    /**
     * 从成功的SQL中学习模式
     */
    public void learnFromSuccessfulSql(String userQuery, String generatedSql, boolean userConfirmed) {
        if (userConfirmed) {
            SqlPattern pattern = patternExtractor.extractPattern(userQuery, generatedSql);
            updateOrCreatePattern(pattern);
            log.info("学习新SQL模式: {}", pattern.getId());
        }
    }
    
    /**
     * 从用户修正中学习
     */
    public void learnFromCorrection(String userQuery, String originalSql, String correctedSql) {
        // 降低错误模式的置信度
        penalizeBadPattern(userQuery, originalSql);
        
        // 学习正确模式
        SqlPattern correctPattern = patternExtractor.extractPattern(userQuery, correctedSql);
        updateOrCreatePattern(correctPattern);
        
        // 分析差异并创建新规则
        analyzeDifferenceAndCreateRules(originalSql, correctedSql);
        
        log.info("从用户修正中学习: 原SQL被降级，新模式已保存");
    }
    
    /**
     * 提取表关联模式
     */
    public void learnTableRelationships(String sql) {
        List<JoinPattern> joins = SqlParser.extractJoins(sql);
        for (JoinPattern join : joins) {
            RelationshipRule rule = RelationshipRule.builder()
                .sourceTable(join.getLeftTable())
                .targetTable(join.getRightTable())
                .joinCondition(join.getCondition())
                .confidence(0.8) // 初始置信度
                .usageCount(1)
                .lastUsed(LocalDateTime.now())
                .learnedFrom("user_sql")
                .build();
                
            configManager.addOrUpdateRelationshipRule(rule);
        }
    }
    
    private void updateOrCreatePattern(SqlPattern pattern) {
        SqlPattern existing = configManager.findSimilarPattern(pattern);
        if (existing != null) {
            // 更新现有模式
            existing.setUsageCount(existing.getUsageCount() + 1);
            existing.setSuccessCount(existing.getSuccessCount() + 1);
            existing.setConfidence(calculateNewConfidence(existing));
            existing.setLastUsed(LocalDateTime.now());
            configManager.updatePattern(existing);
        } else {
            // 创建新模式
            pattern.setConfidence(0.7); // 初始置信度
            pattern.setUsageCount(1);
            pattern.setSuccessCount(1);
            pattern.setCreatedAt(LocalDateTime.now());
            configManager.addPattern(pattern);
        }
    }
}
```

### 4.2 用户反馈处理服务

```java
@Service
@Slf4j
public class FeedbackLearningService {
    
    private final SqlPatternLearningService patternLearningService;
    private final YamlConfigManager configManager;
    
    /**
     * 处理用户确认正确的反馈
     */
    public void handlePositiveFeedback(String userQuery, String generatedSql) {
        patternLearningService.learnFromSuccessfulSql(userQuery, generatedSql, true);
        updatePatternMetrics(userQuery, generatedSql, true);
    }
    
    /**
     * 处理用户报告错误的反馈
     */
    public void handleNegativeFeedback(String userQuery, String generatedSql, String errorReason) {
        updatePatternMetrics(userQuery, generatedSql, false);
        log.warn("用户报告SQL错误: {}, 原因: {}", generatedSql, errorReason);
    }
    
    /**
     * 处理用户修正SQL的反馈
     */
    public void handleCorrectionFeedback(String userQuery, String originalSql, String correctedSql) {
        patternLearningService.learnFromCorrection(userQuery, originalSql, correctedSql);
        
        // 记录修正历史用于后续分析
        CorrectionRecord record = CorrectionRecord.builder()
            .userQuery(userQuery)
            .originalSql(originalSql)
            .correctedSql(correctedSql)
            .timestamp(LocalDateTime.now())
            .build();
        configManager.saveCorrectionRecord(record);
    }
    
    private void updatePatternMetrics(String userQuery, String sql, boolean success) {
        SqlPattern pattern = configManager.findMatchingPattern(userQuery, sql);
        if (pattern != null) {
            if (success) {
                pattern.setSuccessCount(pattern.getSuccessCount() + 1);
            } else {
                pattern.setFailureCount(pattern.getFailureCount() + 1);
            }
            pattern.setConfidence(calculateNewConfidence(pattern));
            configManager.updatePattern(pattern);
        }
    }
}
```

### 4.3 YAML配置管理器

```java
@Component
@Slf4j
public class YamlConfigManager {
    
    private final ObjectMapper yamlMapper;
    private final String relationshipConfigPath = "config/relationship_rules.yml";
    private final String queryPatternConfigPath = "config/query_patterns.yml";
    
    @PostConstruct
    public void init() {
        yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        loadConfigurations();
    }
    
    /**
     * 保存或更新关联关系规则
     */
    public void addOrUpdateRelationshipRule(RelationshipRule rule) {
        RelationshipConfig config = loadRelationshipConfig();
        
        String key = rule.getSourceTable();
        List<RelationshipRule> rules = config.getTableRelationships()
            .computeIfAbsent(key, k -> new ArrayList<>());
        
        // 查找是否存在相同规则
        Optional<RelationshipRule> existing = rules.stream()
            .filter(r -> r.getTargetTable().equals(rule.getTargetTable()))
            .findFirst();
        
        if (existing.isPresent()) {
            // 更新现有规则
            RelationshipRule existingRule = existing.get();
            existingRule.setUsageCount(existingRule.getUsageCount() + 1);
            existingRule.setConfidence(Math.min(0.98, existingRule.getConfidence() + 0.05));
            existingRule.setLastUsed(LocalDateTime.now());
        } else {
            // 添加新规则
            rules.add(rule);
        }
        
        saveRelationshipConfig(config);
        log.info("保存关联关系规则: {}.{} -> {}.{}", 
            rule.getSourceTable(), rule.getSourceColumn(),
            rule.getTargetTable(), rule.getTargetColumn());
    }
    
    /**
     * 保存查询模式
     */
    public void addOrUpdateQueryPattern(SqlPattern pattern) {
        QueryPatternConfig config = loadQueryPatternConfig();
        
        Optional<SqlPattern> existing = config.getQueryIntentions().stream()
            .filter(p -> isSimilarPattern(p, pattern))
            .findFirst();
        
        if (existing.isPresent()) {
            // 更新现有模式
            SqlPattern existingPattern = existing.get();
            existingPattern.setUsageCount(existingPattern.getUsageCount() + 1);
            existingPattern.setConfidence(calculateNewConfidence(existingPattern));
            existingPattern.setLastUsed(LocalDateTime.now());
        } else {
            // 添加新模式
            pattern.setId(generatePatternId());
            config.getQueryIntentions().add(pattern);
        }
        
        saveQueryPatternConfig(config);
    }
    
    /**
     * 根据用户查询找到最匹配的模式
     */
    public List<SqlPattern> findMatchingPatterns(String userQuery) {
        QueryPatternConfig config = loadQueryPatternConfig();
        List<String> queryKeywords = extractKeywords(userQuery);
        
        return config.getQueryIntentions().stream()
            .filter(pattern -> hasKeywordOverlap(pattern.getIntentKeywords(), queryKeywords))
            .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取表关联规则
     */
    public List<RelationshipRule> getRelationshipRules(String sourceTable) {
        RelationshipConfig config = loadRelationshipConfig();
        return config.getTableRelationships().getOrDefault(sourceTable, new ArrayList<>());
    }
}
```

### 4.4 模式匹配和应用引擎

```java
@Service
@Slf4j
public class PatternMatcher {
    
    private final YamlConfigManager configManager;
    
    /**
     * 基于学习到的模式增强SQL生成
     */
    public String enhanceSqlGeneration(String userQuery, String originalSql) {
        String enhancedSql = originalSql;
        
        // 1. 应用关联关系优化
        enhancedSql = applyRelationshipRules(enhancedSql);
        
        // 2. 应用函数模式
        enhancedSql = applyFunctionPatterns(userQuery, enhancedSql);
        
        // 3. 应用查询模式
        enhancedSql = applyQueryPatterns(userQuery, enhancedSql);
        
        return enhancedSql;
    }
    
    /**
     * 应用关联关系规则
     */
    private String applyRelationshipRules(String sql) {
        // 分析SQL中的表
        List<String> tables = SqlParser.extractTables(sql);
        
        for (String table : tables) {
            List<RelationshipRule> rules = configManager.getRelationshipRules(table);
            
            for (RelationshipRule rule : rules) {
                if (rule.getConfidence() > 0.8 && tables.contains(rule.getTargetTable())) {
                    // 如果SQL中包含目标表，且规则置信度高，应用此规则
                    sql = applySingleRelationshipRule(sql, rule);
                }
            }
        }
        
        return sql;
    }
    
    /**
     * 应用函数模式
     */
    private String applyFunctionPatterns(String userQuery, String sql) {
        List<String> queryKeywords = extractKeywords(userQuery);
        List<FunctionPattern> patterns = configManager.getMatchingFunctionPatterns(queryKeywords);
        
        for (FunctionPattern pattern : patterns) {
            if (pattern.getConfidence() > 0.75) {
                sql = applyFunctionPattern(sql, pattern, queryKeywords);
            }
        }
        
        return sql;
    }
    
    /**
     * 获取JOIN建议
     */
    public List<JoinSuggestion> suggestJoins(List<String> tables) {
        List<JoinSuggestion> suggestions = new ArrayList<>();
        
        for (String table : tables) {
            List<RelationshipRule> rules = configManager.getRelationshipRules(table);
            
            for (RelationshipRule rule : rules) {
                if (tables.contains(rule.getTargetTable())) {
                    JoinSuggestion suggestion = JoinSuggestion.builder()
                        .leftTable(rule.getSourceTable())
                        .rightTable(rule.getTargetTable())
                        .joinCondition(rule.getJoinCondition())
                        .confidence(rule.getConfidence())
                        .reason("基于学习规则: " + rule.getLearnedFrom())
                        .examples(Arrays.asList("使用次数: " + rule.getUsageCount()))
                        .build();
                    suggestions.add(suggestion);
                }
            }
        }
        
        return suggestions.stream()
            .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
            .collect(Collectors.toList());
    }
}
```

## 5. 前端集成

### 5.1 反馈界面组件

```vue
<template>
  <div class="sql-result-section">
    <!-- SQL显示区域 -->
    <div class="sql-display">
      <pre class="sql-code"><code>{{ generatedSql }}</code></pre>
    </div>
    
    <!-- 反馈按钮区域 -->
    <div class="feedback-section">
      <div class="feedback-buttons">
        <button @click="confirmCorrect" class="feedback-btn success" :disabled="feedbackSubmitted">
          <i class="fas fa-check"></i>
          <span>正确 ({{ correctCount }})</span>
        </button>
        
        <button @click="reportError" class="feedback-btn error" :disabled="feedbackSubmitted">
          <i class="fas fa-times"></i>
          <span>有问题</span>
        </button>
        
        <button @click="showEditDialog" class="feedback-btn warning">
          <i class="fas fa-edit"></i>
          <span>我来修正</span>
        </button>
      </div>
      
      <!-- 置信度显示 -->
      <div class="confidence-info" v-if="patternInfo">
        <span class="confidence-label">匹配置信度:</span>
        <div class="confidence-bar">
          <div class="confidence-fill" :style="{width: patternInfo.confidence * 100 + '%'}"></div>
        </div>
        <span class="confidence-text">{{ (patternInfo.confidence * 100).toFixed(1) }}%</span>
      </div>
    </div>
    
    <!-- 修正对话框 -->
    <el-dialog v-model="showEditModalFlag" title="SQL修正" width="60%">
      <div class="edit-content">
        <div class="original-sql">
          <h4>原SQL:</h4>
          <pre><code>{{ generatedSql }}</code></pre>
        </div>
        
        <div class="corrected-sql">
          <h4>修正后的SQL:</h4>
          <el-input
            v-model="correctedSql"
            type="textarea"
            :rows="8"
            placeholder="请输入正确的SQL语句"
            class="sql-textarea"
          />
        </div>
        
        <div class="correction-reason">
          <h4>修正原因 (可选):</h4>
          <el-input
            v-model="correctionReason"
            type="textarea"
            :rows="3"
            placeholder="说明修正的原因，帮助系统更好地学习"
          />
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showEditModalFlag = false">取消</el-button>
          <el-button type="primary" @click="submitCorrection">提交修正</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'SqlFeedbackSection',
  props: {
    generatedSql: String,
    userQuery: String,
    patternInfo: Object
  },
  data() {
    return {
      feedbackSubmitted: false,
      showEditModalFlag: false,
      correctedSql: '',
      correctionReason: '',
      correctCount: 0
    }
  },
  methods: {
    async confirmCorrect() {
      try {
        await this.$api.post('/api/mcp/text2sql/feedback/positive', {
          userQuery: this.userQuery,
          generatedSql: this.generatedSql
        });
        
        this.feedbackSubmitted = true;
        this.correctCount++;
        this.$message.success('感谢您的确认，系统已学习此模式！');
        
      } catch (error) {
        this.$message.error('提交反馈失败');
      }
    },
    
    async reportError() {
      try {
        const reason = await this.$prompt('请简述错误原因', '错误反馈', {
          inputPlaceholder: '例如：JOIN条件不正确、缺少WHERE条件等'
        });
        
        await this.$api.post('/api/mcp/text2sql/feedback/negative', {
          userQuery: this.userQuery,
          generatedSql: this.generatedSql,
          errorReason: reason.value
        });
        
        this.feedbackSubmitted = true;
        this.$message.success('错误反馈已提交，系统将改进此类查询！');
        
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('提交反馈失败');
        }
      }
    },
    
    showEditDialog() {
      this.correctedSql = this.generatedSql;
      this.correctionReason = '';
      this.showEditModalFlag = true;
    },
    
    async submitCorrection() {
      if (!this.correctedSql.trim()) {
        this.$message.warning('请输入修正后的SQL');
        return;
      }
      
      try {
        await this.$api.post('/api/mcp/text2sql/feedback/correction', {
          userQuery: this.userQuery,
          originalSql: this.generatedSql,
          correctedSql: this.correctedSql,
          reason: this.correctionReason
        });
        
        this.showEditModalFlag = false;
        this.feedbackSubmitted = true;
        this.$message.success('修正已提交，系统已学习正确的模式！');
        
        // 触发重新生成SQL事件
        this.$emit('sql-corrected', this.correctedSql);
        
      } catch (error) {
        this.$message.error('提交修正失败');
      }
    }
  }
}
</script>
```

### 5.2 反馈API接口

```java
@RestController
@RequestMapping("/api/mcp/text2sql/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {
    
    private final FeedbackLearningService feedbackLearningService;
    
    @PostMapping("/positive")
    public ResponseEntity<ApiResponse> handlePositiveFeedback(@RequestBody PositiveFeedbackRequest request) {
        try {
            feedbackLearningService.handlePositiveFeedback(request.getUserQuery(), request.getGeneratedSql());
            return ResponseEntity.ok(ApiResponse.success("反馈已记录"));
        } catch (Exception e) {
            log.error("处理正面反馈失败", e);
            return ResponseEntity.ok(ApiResponse.error("处理反馈失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/negative")
    public ResponseEntity<ApiResponse> handleNegativeFeedback(@RequestBody NegativeFeedbackRequest request) {
        try {
            feedbackLearningService.handleNegativeFeedback(
                request.getUserQuery(), 
                request.getGeneratedSql(), 
                request.getErrorReason()
            );
            return ResponseEntity.ok(ApiResponse.success("错误反馈已记录"));
        } catch (Exception e) {
            log.error("处理负面反馈失败", e);
            return ResponseEntity.ok(ApiResponse.error("处理反馈失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/correction")
    public ResponseEntity<ApiResponse> handleCorrectionFeedback(@RequestBody CorrectionFeedbackRequest request) {
        try {
            feedbackLearningService.handleCorrectionFeedback(
                request.getUserQuery(),
                request.getOriginalSql(),
                request.getCorrectedSql()
            );
            return ResponseEntity.ok(ApiResponse.success("修正已学习"));
        } catch (Exception e) {
            log.error("处理修正反馈失败", e);
            return ResponseEntity.ok(ApiResponse.error("处理修正失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/patterns/{table}")
    public ResponseEntity<ApiResponse> getTablePatterns(@PathVariable String table) {
        try {
            PatternMatcher patternMatcher = new PatternMatcher(configManager);
            List<RelationshipRule> rules = configManager.getRelationshipRules(table);
            return ResponseEntity.ok(ApiResponse.success(rules));
        } catch (Exception e) {
            log.error("获取表模式失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取模式失败: " + e.getMessage()));
        }
    }
}
```

## 6. 配置自动维护

### 6.1 定时任务

```java
@Component
@Slf4j
public class ConfigMaintenanceScheduler {
    
    private final YamlConfigManager configManager;
    
    /**
     * 每小时更新置信度分数
     */
    @Scheduled(fixedRate = 3600000)
    public void updateConfidenceScores() {
        log.info("开始更新配置置信度分数");
        
        // 更新查询模式置信度
        configManager.getAllQueryPatterns().forEach(pattern -> {
            double newConfidence = calculateTimeDecayedConfidence(pattern);
            if (Math.abs(pattern.getConfidence() - newConfidence) > 0.05) {
                pattern.setConfidence(newConfidence);
                configManager.updateQueryPattern(pattern);
            }
        });
        
        // 更新关联关系置信度
        configManager.getAllRelationshipRules().forEach(rule -> {
            double newConfidence = calculateTimeDecayedConfidence(rule);
            if (Math.abs(rule.getConfidence() - newConfidence) > 0.05) {
                rule.setConfidence(newConfidence);
                configManager.updateRelationshipRule(rule);
            }
        });
        
        log.info("配置置信度分数更新完成");
    }
    
    /**
     * 每天清理低价值配置
     */
    @Scheduled(cron = "0 0 2 * * *") // 每天凌晨2点
    public void cleanupLowValueConfigs() {
        log.info("开始清理低价值配置项");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
        
        // 清理长期未使用且置信度低的模式
        configManager.getAllQueryPatterns().stream()
            .filter(pattern -> pattern.getLastUsed().isBefore(cutoffTime))
            .filter(pattern -> pattern.getConfidence() < 0.3)
            .filter(pattern -> pattern.getUsageCount() < 3)
            .forEach(pattern -> {
                log.info("删除低价值查询模式: {}", pattern.getId());
                configManager.removeQueryPattern(pattern.getId());
            });
        
        // 清理错误的关联关系规则
        configManager.getAllRelationshipRules().stream()
            .filter(rule -> rule.getConfidence() < 0.2)
            .filter(rule -> rule.getUsageCount() < 2)
            .forEach(rule -> {
                log.info("删除低价值关联规则: {}.{} -> {}.{}", 
                    rule.getSourceTable(), rule.getSourceColumn(),
                    rule.getTargetTable(), rule.getTargetColumn());
                configManager.removeRelationshipRule(rule);
            });
        
        log.info("低价值配置清理完成");
    }
    
    private double calculateTimeDecayedConfidence(Object configItem) {
        // 实现时间衰减的置信度计算
        // 考虑使用频率、成功率、时间衰减等因素
        return 0.0; // 具体实现
    }
}
```

## 7. 实施计划

### 7.1 阶段一：基础框架 (1-2周)
- [ ] 实现基础数据结构和实体类
- [ ] 实现YamlConfigManager配置管理器
- [ ] 创建初始的配置文件模板
- [ ] 实现基础的模式匹配功能

### 7.2 阶段二：反馈机制 (1周)
- [ ] 实现FeedbackLearningService
- [ ] 创建反馈API接口
- [ ] 实现前端反馈界面
- [ ] 集成到现有的SQL生成流程

### 7.3 阶段三：模式学习 (1-2周)  
- [ ] 实现SqlPatternLearningService
- [ ] 实现PatternMatcher增强引擎
- [ ] 添加SQL解析和模式提取功能
- [ ] 实现置信度计算算法

### 7.4 阶段四：自动维护 (1周)
- [ ] 实现定时任务调度
- [ ] 添加配置清理和优化逻辑
- [ ] 实现配置备份和恢复
- [ ] 添加系统监控和日志

### 7.5 阶段五：优化完善 (1周)
- [ ] 性能优化和测试
- [ ] 添加更多的SQL模式支持
- [ ] 实现配置导入导出功能
- [ ] 完善文档和使用指南

## 8. 监控和评估

### 8.1 效果指标
- SQL生成准确率提升
- 用户正面反馈比例
- 修正频率下降趋势
- 配置规则覆盖率

### 8.2 系统监控
- 配置文件大小和加载性能
- 模式匹配耗时统计
- 反馈处理成功率
- 定时任务执行状况

这个设计方案提供了一个完整的、可持续改进的SQL生成优化系统，通过用户反馈不断学习和完善，同时保持轻量级和高可维护性。