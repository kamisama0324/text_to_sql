package com.kami.springai.text2sql.service;

import com.kami.springai.common.cache.SchemaCache;
import com.kami.springai.text2sql.config.EnhancedConfigurationManager;
import com.kami.springai.text2sql.model.*;
import com.kami.springai.text2sql.validator.SqlValidationPipeline;
import com.kami.springai.text2sql.validator.ValidationPipelineResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Text2SQL转换服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Text2SqlService {

    private final ChatClient.Builder chatClientBuilder;
    private final SchemaDiscoveryService schemaDiscoveryService;
    private final SchemaCache schemaCache;
    private final SemanticAnalyzer semanticAnalyzer;
    private final GeneralizedLearner generalizedLearner;
    private final DualPatternManager dualPatternManager;
    private final SqlValidationPipeline validationPipeline;
    private final ContextualPromptBuilder promptBuilder;
    private final EnhancedConfigurationManager enhancedConfig;

    /**
     * 系统Prompt模板
     */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的SQL查询生成助手，具备以下能力：

            1. **角色定位**：根据自然语言描述生成准确的MySQL SQL查询语句
            2. **严格安全约束**：只能生成SELECT查询语句，严禁生成任何包含以下关键词的语句：
               INSERT, UPDATE, DELETE, DROP, ALTER, CREATE, TRUNCATE, REPLACE, MERGE, CALL, EXEC
            3. **查询优化**：生成高效、规范的SQL语句，合理使用索引和连接
            4. **错误处理**：对于不明确的查询需求，会要求用户澄清

            **输出格式要求**：
            - 只返回纯净的SELECT SQL语句，不要任何解释文字、代码块标记或注释
            - 使用标准的MySQL语法
            - 表名和字段名使用反引号包围
            - 自动添加合理的LIMIT限制（默认100条）
            - 对于模糊查询使用LIKE操作符
            - 绝对不要使用CREATE TABLE、CREATE VIEW等任何CREATE语句

            **示例**：
            用户："查询所有用户"
            你的回答：SELECT * FROM `users` LIMIT 100;

            用户："查询年龄大于25的用户姓名"
            你的回答：SELECT `name` FROM `users` WHERE `age` > 25 LIMIT 100;
            """;

    /**
     * 将自然语言查询转换为SQL语句（集成学习框架和可靠性验证）
     */
    public String convertToSql(String userQuery, String context) {
        log.info("开始转换自然语言查询为SQL (含学习框架和可靠性验证): {}", userQuery);

        try {
            // 1. 获取数据库结构（使用缓存）
            // 使用数据源ID作为缓存键，确保与UUID数据源ID正确关联
            String dataSourceId = com.kami.springai.datasource.service.DataSourceContextHolder.getDataSourceId();
            DatabaseSchema schema = schemaCache.getSchema(dataSourceId);

            // 2. 语义分析
            QuerySemantic semantic = semanticAnalyzer.analyzeQuery(userQuery, schema.getTables());
            log.info("语义分析完成，置信度: {:.2f}", semantic.getConfidence());

            String sql = null;
            GeneralizedSqlPattern usedPattern = null;
            ValidationPipelineResult validationResult = null;
            int attemptCount = 0;
            int maxAttempts = 3;

            // 3. 多次尝试生成和验证SQL，直到通过验证或达到最大尝试次数
            while (attemptCount < maxAttempts) {
                attemptCount++;
                log.info("SQL生成尝试 {}/{}", attemptCount, maxAttempts);

                // 3.1. 尝试从学习模式中匹配
                List<String> intentSemantics = List.of(semantic.getIntent().getPrimaryIntent());
                List<String> entityTypes = semantic.getEntities().stream()
                        .map(EntitySemantic::getSemanticType)
                        .collect(Collectors.toList());

                List<GeneralizedSqlPattern> matchingPatterns = dualPatternManager
                        .findMatchingPatterns(intentSemantics, entityTypes);

                // 3.2. 如果找到高置信度模式，优先使用
                if (sql == null && !matchingPatterns.isEmpty()) {
                    GeneralizedSqlPattern bestPattern = matchingPatterns.get(0);
                    if (bestPattern.isHighQualityPattern() && bestPattern.getGeneralConfidence() > 0.8) {
                        log.info("使用学习模式生成SQL: {}", bestPattern.getPatternName());
                        sql = applyPatternToGenerate(bestPattern, semantic, schema);
                        usedPattern = bestPattern;
                    }
                }

                // 3.3. 如果没有合适模式，使用AI生成
                if (sql == null) {
                    log.info("使用AI生成SQL");
                    sql = generateSqlWithAI(userQuery, schema, semantic, context);
                }

                // 3.4. 清理生成的SQL
                sql = cleanGeneratedSql(sql);

                // 3.5. 执行可靠性验证流水线
                log.info("执行SQL可靠性验证");
                validationResult = validationPipeline.validateSql(sql, userQuery, schema);

                // 3.6. 检查验证结果
                if (validationResult.isOverallValid()) {
                    // 验证通过，记录成功
                    log.info("SQL验证通过: {}", validationResult.getOverallMessage());
                    break;
                } else if (validationResult.hasCriticalErrors()) {
                    // 严重错误，立即终止
                    log.error("SQL存在严重错误: {}", validationResult.getOverallMessage());
                    throw new RuntimeException("SQL生成失败，存在严重错误: " + validationResult.getOverallMessage());
                } else {
                    // 有错误但不是严重错误，尝试修复
                    log.warn("SQL验证失败 (尝试 {}/{}): {}", attemptCount, maxAttempts, validationResult.getOverallMessage());

                    if (attemptCount < maxAttempts) {
                        // 根据验证结果尝试修复SQL
                        String improvedSql = attemptSqlImprovement(sql, userQuery, validationResult, schema, semantic);
                        if (improvedSql != null && !improvedSql.equals(sql)) {
                            sql = improvedSql;
                            log.info("尝试使用改进的SQL: {}", sql);
                            continue; // 重新验证改进后的SQL
                        }
                    }

                    // 如果无法改进或达到最大尝试次数，清空sql以触发重新生成
                    sql = null;
                    usedPattern = null;
                }
            }

            // 4. 最终检查
            if (sql == null || !validationResult.isOverallValid()) {
                String errorMsg = validationResult != null ? validationResult.getOverallMessage() : "无法生成有效的SQL语句";
                throw new RuntimeException("SQL生成失败: " + errorMsg);
            }

            // 5. 基本安全验证（保留原有逻辑作为最后防线）
            validateGeneratedSql(sql);

            // 6. 记录成功使用的模式
            if (usedPattern != null && !usedPattern.getPatternId().startsWith("base_")) {
                usedPattern.recordSuccess("mysql");
                log.debug("模式使用记录: {}", usedPattern.getPatternId());
            }

            // 7. 记录验证结果
            if (validationResult.hasWarnings()) {
                log.warn("SQL生成成功但有警告: {}", String.join("; ", validationResult.getAllWarningMessages()));
            }

            log.info("SQL生成成功 (尝试{}次，验证耗时{}ms): {}",
                    attemptCount, validationResult.getTotalExecutionTimeMs(), sql);
            return sql;

        } catch (Exception e) {
            log.error("Text2SQL转换失败", e);
            throw new RuntimeException("SQL生成失败: " + e.getMessage(), e);
        }
    }

    // 注入具体的 ChatModel 以支持动态选择
    // Note: Gemini support temporarily disabled
    private final org.springframework.ai.deepseek.DeepSeekChatModel deepSeekChatModel;

    // 简化版本的辅助方法
    private String generateSqlWithAI(String userQuery, DatabaseSchema schema, QuerySemantic semantic, String context) {
        try {
            String systemPrompt = promptBuilder.buildEnhancedSystemPrompt(semantic, schema);
            String userPrompt = promptBuilder.buildContextualUserPrompt(userQuery, semantic, schema, new ArrayList<>());

            // 当前仅使用 DeepSeek 模型
            log.info("使用 DeepSeek 模型处理查询");
            ChatClient clientToUse = ChatClient.builder(deepSeekChatModel)
                    .defaultSystem(systemPrompt)
                    .build();

            return clientToUse.prompt()
                    .user(userPrompt)
                    .call()
                    .content()
                    .trim();
        } catch (Exception e) {
            log.warn("增强AI生成失败，使用基础模式: {}", e.getMessage());
            return generateBasicSql(userQuery, schema, context);
        }
    }

    private String generateBasicSql(String userQuery, DatabaseSchema schema, String context) {
        String schemaDescription = generateSchemaForAI(schema);

        String userPrompt = String.format("""
                **数据库结构信息**：
                %s

                **用户查询需求**：%s

                %s

                请根据以上数据库结构，为用户的查询需求生成对应的SQL语句。
                """, schemaDescription, userQuery, context != null ? "**额外上下文**：" + context : "");

        ChatClient chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content()
                .trim();
    }

    private String applyPatternToGenerate(GeneralizedSqlPattern pattern, QuerySemantic semantic,
            DatabaseSchema schema) {
        // 简化的模式应用，实际应用中会更复杂
        log.debug("应用模式: {}", pattern.getPatternName());

        // 如果模式应用失败，回退到AI生成
        return generateBasicSql("查询数据", schema, null);
    }

    private String attemptSqlImprovement(String originalSql, String userQuery,
            ValidationPipelineResult validationResult,
            DatabaseSchema schema, QuerySemantic semantic) {
        try {
            List<String> errorMessages = validationResult.getAllErrorMessages();
            String improvementPrompt = promptBuilder.buildErrorFixPrompt(
                    originalSql, userQuery, errorMessages, semantic, schema);

            ChatClient chatClient = chatClientBuilder
                    .defaultSystem(SYSTEM_PROMPT)
                    .build();

            String improvedSql = chatClient.prompt()
                    .user(improvementPrompt)
                    .call()
                    .content()
                    .trim();

            return cleanGeneratedSql(improvedSql);
        } catch (Exception e) {
            log.warn("SQL改进尝试失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 添加用户反馈学习接口
     */
    public void learnFromFeedback(String userQuery, String generatedSql, boolean isCorrect,
            String correctedSql, String database) {
        log.info("接收用户反馈学习: 查询='{}', 正确={}", userQuery, isCorrect);

        try {
            // 使用数据源ID作为缓存键，保持与其他方法一致
            String dataSourceId = com.kami.springai.datasource.service.DataSourceContextHolder.getDataSourceId();
            DatabaseSchema schema = schemaCache.getSchema(dataSourceId);

            // 重新分析语义（用于学习）
            QuerySemantic semantic = semanticAnalyzer.analyzeQuery(userQuery, schema.getTables());

            // 使用双模式管理器进行学习
            YamlConfigManager.FeedbackType feedbackType;
            if (isCorrect) {
                feedbackType = YamlConfigManager.FeedbackType.POSITIVE;
            } else if (correctedSql != null && !correctedSql.trim().isEmpty()) {
                feedbackType = YamlConfigManager.FeedbackType.CORRECTION;
            } else {
                feedbackType = YamlConfigManager.FeedbackType.NEGATIVE;
            }

            dualPatternManager.learnFromUserFeedback(
                    feedbackType, userQuery, generatedSql, correctedSql, semantic);

            log.info("用户反馈学习完成");

        } catch (Exception e) {
            log.error("用户反馈学习失败", e);
        }
    }

    /**
     * 解释SQL语句的含义
     */
    public String explainSql(String sql, String context) {
        log.info("开始解释SQL语句: {}", sql);

        try {
            // 使用数据源ID而不是数据库名称作为缓存键
            String dataSourceId = com.kami.springai.datasource.service.DataSourceContextHolder.getDataSourceId();
            DatabaseSchema schema = schemaCache.getSchema(dataSourceId);
            String schemaDescription = generateSchemaForAI(schema);

            String explainPrompt = String.format("""
                    请用简洁的中文解释以下SQL语句的含义和作用：

                    数据库结构：
                    %s

                    SQL语句：
                    %s

                    %s

                    解释要求：
                    1. 说明查询的目的和结果
                    2. 涉及哪些表和字段
                    3. 使用了哪些查询条件
                    4. 预期返回什么样的数据
                    """, schemaDescription, sql, context != null ? "额外上下文：" + context : "");

            ChatClient chatClient = chatClientBuilder.build();
            String explanation = chatClient.prompt()
                    .user(explainPrompt)
                    .call()
                    .content();

            log.info("SQL解释生成完成");
            return explanation.trim();

        } catch (Exception e) {
            log.error("SQL解释生成失败", e);
            throw new RuntimeException("SQL解释失败: " + e.getMessage(), e);
        }
    }

    /**
     * 为AI生成简化的数据库结构描述
     */
    private String generateSchemaForAI(DatabaseSchema schema) {
        StringBuilder description = new StringBuilder();

        description.append(String.format("数据库: %s\n\n", schema.getDatabaseName()));

        for (DatabaseSchema.Table table : schema.getTables()) {
            description.append(String.format("表: %s", table.getName()));

            // 添加表注释
            if (table.getComment() != null && !table.getComment().trim().isEmpty()) {
                description.append(String.format(" (%s)", table.getComment()));
            }
            description.append("\n");

            // 字段信息（只保留关键信息）
            String columns = table.getColumns().stream()
                    .map(col -> {
                        StringBuilder colInfo = new StringBuilder(col.getName());
                        colInfo.append(" ").append(col.getType());

                        if (col.isPrimaryKey())
                            colInfo.append(" [PK]");
                        if (!col.isNullable())
                            colInfo.append(" [NOT NULL]");
                        if (col.getComment() != null && !col.getComment().trim().isEmpty()) {
                            colInfo.append(" // ").append(col.getComment());
                        }

                        return colInfo.toString();
                    })
                    .collect(Collectors.joining(", "));

            description.append("  字段: ").append(columns).append("\n");

            // 外键关系
            if (table.getForeignKeys() != null && !table.getForeignKeys().isEmpty()) {
                String relationships = table.getForeignKeys().stream()
                        .map(fk -> fk.getRelationshipDescription())
                        .collect(Collectors.joining(", "));
                description.append("  关联: ").append(relationships).append("\n");
            }

            description.append("\n");
        }

        return description.toString();
    }

    /**
     * 清理AI生成的SQL语句
     */
    private String cleanGeneratedSql(String sql) {
        if (sql == null) {
            throw new RuntimeException("AI未生成有效的SQL语句");
        }

        // 移除markdown代码块标记
        sql = sql.replaceAll("```sql\\n?", "").replaceAll("```", "");

        // 移除多余的空白字符
        sql = sql.trim();

        // 确保以分号结尾
        if (!sql.endsWith(";")) {
            sql += ";";
        }

        return sql;
    }

    /**
     * 验证生成的SQL语句的安全性
     */
    /**
     * 验证生成的SQL语句的安全性 (已委托给 SqlValidationPipeline 处理)
     */
    private void validateGeneratedSql(String sql) {
        // 保留此空方法或完全移除，因为主要验证逻辑已移至 SqlValidationPipeline
        // 为了保持接口兼容性，暂时保留为空检查，或者进行最基本的非空检查
        if (sql == null || sql.trim().isEmpty()) {
            throw new RuntimeException("生成的SQL为空");
        }
    }
}