package com.kami.springai.text2sql.service;

import com.kami.springai.text2sql.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 上下文提示词构建器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextualPromptBuilder {

    /**
     * 构建增强的系统提示词
     */
    public String buildEnhancedSystemPrompt(QuerySemantic semantic, DatabaseSchema schema) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("""
            你是一个专业的SQL查询生成助手，具备以下能力：
            
            1. **角色定位**：根据自然语言描述生成准确的MySQL SQL查询语句
            2. **严格安全约束**：只能生成SELECT查询语句
            3. **查询优化**：生成高效、规范的SQL语句
            
            **输出格式要求**：
            - 只返回纯净的SELECT SQL语句，不要任何解释文字
            - 使用标准的MySQL语法
            - 表名和字段名使用反引号包围
            - 自动添加合理的LIMIT限制（默认100条）
            """);
        
        // 添加特定的上下文信息
        if (semantic != null && semantic.getIntent() != null) {
            String queryType = semantic.getIntent().getQueryType();
            switch (queryType) {
                case "COUNT", "AGGREGATION" -> prompt.append("\n**特殊要求**：使用聚合函数进行统计分析");
                case "JOIN" -> prompt.append("\n**特殊要求**：正确使用JOIN语法连接相关表");
                case "GROUP_BY" -> prompt.append("\n**特殊要求**：合理使用GROUP BY进行分组");
            }
        }
        
        return prompt.toString();
    }

    /**
     * 构建上下文用户提示词
     */
    public String buildContextualUserPrompt(String userQuery, QuerySemantic semantic, 
                                           DatabaseSchema schema, List<GeneralizedSqlPattern> patterns) {
        StringBuilder prompt = new StringBuilder();
        
        // 数据库结构信息
        prompt.append("**数据库结构信息**：\n");
        prompt.append(generateSchemaDescription(schema));
        prompt.append("\n\n");
        
        // 用户查询需求
        prompt.append("**用户查询需求**：").append(userQuery).append("\n\n");
        
        // 语义分析结果
        if (semantic != null) {
            prompt.append("**查询语义分析**：\n");
            appendSemanticInfo(prompt, semantic);
            prompt.append("\n");
        }
        
        // 相似模式参考
        if (!patterns.isEmpty()) {
            prompt.append("**相似模式参考**：\n");
            appendPatternInfo(prompt, patterns);
            prompt.append("\n");
        }
        
        prompt.append("**生成要求**：请根据以上信息生成准确、高效的SQL查询语句。");
        
        return prompt.toString();
    }

    /**
     * 构建错误修复提示词
     */
    public String buildErrorFixPrompt(String originalSql, String userQuery, List<String> errorMessages,
                                    QuerySemantic semantic, DatabaseSchema schema) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("**SQL错误修复任务**\n\n");
        
        prompt.append("**原始SQL**：\n").append(originalSql).append("\n\n");
        
        prompt.append("**用户原始需求**：").append(userQuery).append("\n\n");
        
        prompt.append("**发现的错误**：\n");
        for (int i = 0; i < errorMessages.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, errorMessages.get(i)));
        }
        prompt.append("\n");
        
        prompt.append("**数据库结构参考**：\n");
        prompt.append(generateSchemaDescription(schema));
        prompt.append("\n\n");
        
        if (semantic != null) {
            prompt.append("**语义分析参考**：\n");
            appendSemanticInfo(prompt, semantic);
            prompt.append("\n");
        }
        
        prompt.append("**修复要求**：请生成修复后的正确SQL语句，确保语法正确、语义合理、性能优化。");
        
        return prompt.toString();
    }

    private String generateSchemaDescription(DatabaseSchema schema) {
        StringBuilder description = new StringBuilder();
        
        description.append(String.format("数据库: %s\n\n", schema.getDatabaseName()));
        
        for (DatabaseSchema.Table table : schema.getTables()) {
            description.append(String.format("表: %s", table.getName()));
            
            if (table.getComment() != null && !table.getComment().trim().isEmpty()) {
                description.append(String.format(" (%s)", table.getComment()));
            }
            description.append("\n");
            
            // 字段信息
            String columns = table.getColumns().stream()
                    .map(col -> {
                        StringBuilder colInfo = new StringBuilder(col.getName());
                        colInfo.append(" ").append(col.getType());
                        
                        if (col.isPrimaryKey()) colInfo.append(" [PK]");
                        if (!col.isNullable()) colInfo.append(" [NOT NULL]");
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

    private void appendSemanticInfo(StringBuilder prompt, QuerySemantic semantic) {
        if (semantic.getIntent() != null) {
            prompt.append("- 查询类型：").append(semantic.getIntent().getQueryType()).append("\n");
            prompt.append("- 主要意图：").append(semantic.getIntent().getPrimaryIntent()).append("\n");
            prompt.append("- 置信度：").append(String.format("%.2f", semantic.getConfidence())).append("\n");
        }
        
        if (semantic.getEntities() != null && !semantic.getEntities().isEmpty()) {
            prompt.append("- 涉及实体：");
            String entities = semantic.getEntities().stream()
                    .map(EntitySemantic::getSemanticType)
                    .collect(Collectors.joining(", "));
            prompt.append(entities).append("\n");
        }
        
        if (semantic.getConditions() != null && !semantic.getConditions().isEmpty()) {
            prompt.append("- 查询条件：");
            String conditions = semantic.getConditions().stream()
                    .map(cond -> String.format("%s %s %s", 
                            cond.getField() != null ? cond.getField() : "unknown", 
                            cond.getOperator() != null ? cond.getOperator() : "unknown", 
                            cond.getValue() != null ? cond.getValue() : "unknown"))
                    .collect(Collectors.joining(", "));
            prompt.append(conditions).append("\n");
        }
    }

    private void appendPatternInfo(StringBuilder prompt, List<GeneralizedSqlPattern> patterns) {
        for (int i = 0; i < Math.min(3, patterns.size()); i++) {
            GeneralizedSqlPattern pattern = patterns.get(i);
            prompt.append(String.format("- %s (置信度: %.2f)\n", 
                    pattern.getPatternName(), pattern.getGeneralConfidence()));
        }
    }
}