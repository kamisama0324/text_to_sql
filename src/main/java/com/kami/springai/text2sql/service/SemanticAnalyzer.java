package com.kami.springai.text2sql.service;

import com.kami.springai.text2sql.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 语义分析器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticAnalyzer {

    /**
     * 分析查询语义
     */
    public QuerySemantic analyzeQuery(String userQuery, List<DatabaseSchema.Table> tables) {
        log.debug("开始语义分析: {}", userQuery);
        
        try {
            // 分析意图
            IntentSemantic intent = analyzeIntent(userQuery, tables);
            
            // 分析实体
            List<EntitySemantic> entities = analyzeEntities(userQuery, tables);
            
            // 分析条件
            List<ConditionSemantic> conditions = analyzeConditions(userQuery);
            
            // 计算整体置信度
            double confidence = calculateConfidence(intent, entities, conditions);
            
            return QuerySemantic.builder()
                    .intent(intent)
                    .entities(entities)
                    .conditions(conditions)
                    .confidence(confidence)
                    .build();
                    
        } catch (Exception e) {
            log.error("语义分析失败: {}", e.getMessage(), e);
            return createDefaultSemantic(userQuery, tables);
        }
    }

    private IntentSemantic analyzeIntent(String userQuery, List<DatabaseSchema.Table> tables) {
        String query = userQuery.toLowerCase();
        
        // 判断查询类型
        String queryType = "SIMPLE_SELECT";
        if (query.contains("统计") || query.contains("计算") || query.contains("数量")) {
            queryType = "COUNT";
        } else if (query.contains("平均") || query.contains("最大") || query.contains("最小") || query.contains("求和")) {
            queryType = "AGGREGATION";
        } else if (query.contains("分组") || query.contains("按照")) {
            queryType = "GROUP_BY";
        } else if (query.contains("关联") || query.contains("连接") || query.contains("和")) {
            queryType = "JOIN";
        }
        
        // 找到相关表
        List<String> relevantTables = new ArrayList<>();
        for (DatabaseSchema.Table table : tables) {
            if (query.contains(table.getName().toLowerCase()) || 
                (table.getComment() != null && query.contains(table.getComment()))) {
                relevantTables.add(table.getName());
            }
        }
        
        // 如果没有明确的表，尝试根据常见词汇推断
        if (relevantTables.isEmpty()) {
            if (query.contains("用户")) relevantTables.add("users");
            if (query.contains("订单")) relevantTables.add("orders");
            if (query.contains("产品") || query.contains("商品")) relevantTables.add("products");
        }
        
        return IntentSemantic.builder()
                .primaryIntent(extractPrimaryIntent(query))
                .queryType(queryType)
                .relevantTables(relevantTables)
                .intentSemantics(List.of(queryType.toLowerCase()))
                .complexQuery(queryType.equals("JOIN") || queryType.equals("GROUP_BY"))
                .build();
    }

    private List<EntitySemantic> analyzeEntities(String userQuery, List<DatabaseSchema.Table> tables) {
        List<EntitySemantic> entities = new ArrayList<>();
        String query = userQuery.toLowerCase();
        
        for (DatabaseSchema.Table table : tables) {
            String tableName = table.getName().toLowerCase();
            
            // 检查表名或注释是否在查询中
            if (query.contains(tableName) || 
                (table.getComment() != null && query.contains(table.getComment().toLowerCase()))) {
                
                entities.add(EntitySemantic.builder()
                        .entityName(table.getName())
                        .semanticType(tableName)
                        .tableName(table.getName())
                        .primary(entities.isEmpty()) // 第一个找到的表作为主要实体
                        .confidence(0.8)
                        .build());
            }
        }
        
        // 如果没找到实体，创建默认实体
        if (entities.isEmpty() && !tables.isEmpty()) {
            entities.add(EntitySemantic.builder()
                    .entityName(tables.get(0).getName())
                    .semanticType("default")
                    .tableName(tables.get(0).getName())
                    .primary(true)
                    .confidence(0.5)
                    .build());
        }
        
        return entities;
    }

    private List<ConditionSemantic> analyzeConditions(String userQuery) {
        List<ConditionSemantic> conditions = new ArrayList<>();
        String query = userQuery.toLowerCase();
        
        // 简单的条件识别
        if (query.contains("大于") || query.contains(">")) {
            conditions.add(ConditionSemantic.builder()
                    .field("unknown")
                    .operator(">")
                    .value("unknown")
                    .conditionType("COMPARISON")
                    .build());
        }
        
        if (query.contains("等于") || query.contains("=") || query.contains("是")) {
            conditions.add(ConditionSemantic.builder()
                    .field("unknown")
                    .operator("=")
                    .value("unknown")
                    .conditionType("EQUALITY")
                    .build());
        }
        
        if (query.contains("包含") || query.contains("like")) {
            conditions.add(ConditionSemantic.builder()
                    .field("unknown")
                    .operator("LIKE")
                    .value("unknown")
                    .conditionType("PATTERN")
                    .build());
        }
        
        return conditions;
    }

    private String extractPrimaryIntent(String query) {
        if (query.contains("查询") || query.contains("查找") || query.contains("获取")) {
            return "query";
        } else if (query.contains("统计") || query.contains("计算")) {
            return "count";
        } else if (query.contains("分析")) {
            return "analyze";
        } else {
            return "query"; // 默认意图
        }
    }

    private double calculateConfidence(IntentSemantic intent, List<EntitySemantic> entities, List<ConditionSemantic> conditions) {
        double baseConfidence = 0.6;
        
        // 如果找到了相关表，增加置信度
        if (!intent.getRelevantTables().isEmpty()) {
            baseConfidence += 0.2;
        }
        
        // 如果找到了实体，增加置信度
        if (!entities.isEmpty()) {
            baseConfidence += 0.1;
        }
        
        // 如果有明确的条件，增加置信度
        if (!conditions.isEmpty()) {
            baseConfidence += 0.1;
        }
        
        return Math.min(1.0, baseConfidence);
    }

    private QuerySemantic createDefaultSemantic(String userQuery, List<DatabaseSchema.Table> tables) {
        return QuerySemantic.builder()
                .intent(IntentSemantic.builder()
                        .primaryIntent("query")
                        .queryType("SIMPLE_SELECT")
                        .relevantTables(tables.isEmpty() ? List.of() : List.of(tables.get(0).getName()))
                        .intentSemantics(List.of("simple_select"))
                        .complexQuery(false)
                        .build())
                .entities(tables.isEmpty() ? List.of() : List.of(
                        EntitySemantic.builder()
                                .entityName(tables.get(0).getName())
                                .semanticType("default")
                                .tableName(tables.get(0).getName())
                                .primary(true)
                                .confidence(0.5)
                                .build()))
                .conditions(List.of())
                .confidence(0.5)
                .build();
    }
}