package com.kami.springai.text2sql.config;

import com.kami.springai.text2sql.model.QuerySemantic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 增强配置管理器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedConfigurationManager {

    /**
     * 获取智能提示词模板
     */
    public String getIntelligentPromptTemplate(String scenarioType, String queryType, String complexity) {
        log.debug("获取智能提示词模板: scenario={}, type={}, complexity={}", scenarioType, queryType, complexity);
        
        // 根据不同场景返回不同的模板
        return switch (scenarioType) {
            case "list_query" -> getListQueryTemplate(queryType, complexity);
            case "detail_query" -> getDetailQueryTemplate(queryType, complexity);
            case "analytics" -> getAnalyticsTemplate(queryType, complexity);
            default -> getGeneralTemplate(queryType, complexity);
        };
    }

    /**
     * 获取错误修复提示词
     */
    public String getErrorFixingPrompt(String errorType, Map<String, Object> errorDetails) {
        log.debug("获取错误修复提示词: {}", errorType);
        
        return switch (errorType) {
            case "SYNTAX_ERROR" -> getSyntaxErrorFixPrompt(errorDetails);
            case "SEMANTIC_ERROR" -> getSemanticErrorFixPrompt(errorDetails);
            case "PERFORMANCE_ERROR" -> getPerformanceErrorFixPrompt(errorDetails);
            default -> getGeneralErrorFixPrompt(errorDetails);
        };
    }

    /**
     * 获取数据库优化建议
     */
    public List<String> getDatabaseOptimizationSuggestions(String sql, String queryType) {
        return List.of(
            "使用索引来提高查询性能",
            "避免SELECT *，明确指定需要的列",
            "使用LIMIT限制返回行数",
            "合理使用WHERE条件过滤数据"
        );
    }

    /**
     * 获取增强模式建议
     */
    public List<Map<String, Object>> getEnhancedPatternSuggestions(String queryType, List<String> entityTypes) {
        return List.of(
            Map.of("patternName", "基础查询模式", "confidence", 0.8),
            Map.of("patternName", "关联查询模式", "confidence", 0.7)
        );
    }

    private String getListQueryTemplate(String queryType, String complexity) {
        return """
            你是一个专业的SQL生成专家，专门处理列表查询。
            
            **任务**：生成简洁高效的SELECT查询语句
            **要求**：
            - 优先使用具体列名而非SELECT *
            - 自动添加合理的LIMIT限制
            - 考虑使用索引优化性能
            
            **输出格式**：只返回纯SQL语句，不要解释
            """;
    }

    private String getDetailQueryTemplate(String queryType, String complexity) {
        return """
            你是一个专业的SQL生成专家，专门处理详细查询和关联查询。
            
            **任务**：生成包含JOIN的复杂查询语句
            **要求**：
            - 正确使用JOIN语法连接相关表
            - 处理外键关系
            - 优化JOIN性能
            
            **输出格式**：只返回纯SQL语句，不要解释
            """;
    }

    private String getAnalyticsTemplate(String queryType, String complexity) {
        return """
            你是一个专业的SQL生成专家，专门处理统计分析查询。
            
            **任务**：生成包含聚合函数的分析查询
            **要求**：
            - 正确使用COUNT、SUM、AVG等聚合函数
            - 合理使用GROUP BY和HAVING
            - 考虑性能优化
            
            **输出格式**：只返回纯SQL语句，不要解释
            """;
    }

    private String getGeneralTemplate(String queryType, String complexity) {
        return """
            你是一个专业的SQL查询生成助手。
            
            **任务**：根据自然语言描述生成准确的MySQL SQL查询语句
            **要求**：
            - 只生成SELECT查询语句
            - 使用标准MySQL语法
            - 表名和字段名使用反引号包围
            - 自动添加LIMIT限制
            
            **输出格式**：只返回纯SQL语句，不要解释
            """;
    }

    /**
     * 智能选择最佳模型
     * 根据查询复杂度和配置选择合适的模型提供商
     */
    public String selectBestModelProvider(String queryType, String complexity, boolean isGeminiAvailable) {
        // 如果Gemini不可用，强制使用OpenAI (DeepSeek)
        if (!isGeminiAvailable) {
            return "openai";
        }
        
        // 简单查询优先使用 Gemini Flash (速度快，成本低)
        if ("simple".equalsIgnoreCase(complexity) || "list_query".equalsIgnoreCase(queryType)) {
            return "gemini";
        }
        
        // 复杂分析或详细查询使用 DeepSeek (推理能力强)
        if ("complex".equalsIgnoreCase(complexity) || "analytics".equalsIgnoreCase(queryType)) {
            return "openai";
        }
        
        // 默认使用配置的提供商
        return "default";
    }

    private String getSyntaxErrorFixPrompt(Map<String, Object> errorDetails) {
        return String.format("""
            发现SQL语法错误，请修复以下问题：
            
            **原始SQL**: %s
            **错误信息**: %s
            
            请生成修复后的正确SQL语句。
            """, 
            errorDetails.get("originalSql"), 
            errorDetails.get("errorAnalysis"));
    }

    private String getSemanticErrorFixPrompt(Map<String, Object> errorDetails) {
        return String.format("""
            发现SQL语义错误，请修复以下问题：
            
            **原始SQL**: %s
            **数据库结构**: %s
            **错误信息**: %s
            
            请根据实际数据库结构生成正确的SQL语句。
            """, 
            errorDetails.get("originalSql"), 
            errorDetails.get("schemaInfo"),
            errorDetails.get("errorAnalysis"));
    }

    private String getPerformanceErrorFixPrompt(Map<String, Object> errorDetails) {
        return String.format("""
            发现SQL性能问题，请优化以下查询：
            
            **原始SQL**: %s
            **性能问题**: %s
            
            请生成优化后的高性能SQL语句。
            """, 
            errorDetails.get("originalSql"), 
            errorDetails.get("errorAnalysis"));
    }

    private String getGeneralErrorFixPrompt(Map<String, Object> errorDetails) {
        return String.format("""
            发现SQL错误，请修复：
            
            **原始SQL**: %s
            **用户需求**: %s
            **错误分析**: %s
            
            请生成修复后的正确SQL语句。
            """, 
            errorDetails.get("originalSql"), 
            errorDetails.get("userQuery"),
            errorDetails.get("errorAnalysis"));
    }
}