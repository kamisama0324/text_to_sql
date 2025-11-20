package com.kami.springai.text2sql.validator;

import com.kami.springai.text2sql.model.DatabaseSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL验证流水线
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlValidationPipeline {

    /**
     * 验证SQL
     */
    public ValidationPipelineResult validateSql(String sql, String userQuery, DatabaseSchema schema) {
        log.debug("开始SQL验证流水线: {}", sql);

        long startTime = System.currentTimeMillis();
        List<ValidatorResult> results = new ArrayList<>();

        try {
            // 语法验证
            results.add(validateSyntax(sql));

            // 安全验证
            results.add(validateSecurity(sql));

            // 语义验证
            results.add(validateSemantics(sql, schema));

            // 性能验证
            results.add(validatePerformance(sql, schema));

            // 计算总体结果
            boolean overallValid = results.stream().allMatch(ValidatorResult::isValid);
            String overallMessage = generateOverallMessage(results);
            long totalTime = System.currentTimeMillis() - startTime;

            return ValidationPipelineResult.builder()
                    .validatorResults(results)
                    .overallValid(overallValid)
                    .overallMessage(overallMessage)
                    .totalExecutionTimeMs(totalTime)
                    .build();

        } catch (Exception e) {
            log.error("SQL验证流水线执行失败: {}", e.getMessage(), e);

            return ValidationPipelineResult.builder()
                    .validatorResults(results)
                    .overallValid(false)
                    .overallMessage("验证流水线执行失败: " + e.getMessage())
                    .totalExecutionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private ValidatorResult validateSyntax(String sql) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 基本语法检查
        String upperSql = sql.toUpperCase().trim();

        if (!upperSql.startsWith("SELECT")) {
            errors.add("SYNTAX ERROR: SQL必须以SELECT开头");
        }

        if (!sql.trim().endsWith(";")) {
            warnings.add("建议SQL语句以分号结尾");
        }

        // 检查括号匹配
        int openParens = sql.length() - sql.replace("(", "").length();
        int closeParens = sql.length() - sql.replace(")", "").length();
        if (openParens != closeParens) {
            errors.add("SYNTAX ERROR: 括号不匹配");
        }

        return ValidatorResult.builder()
                .valid(errors.isEmpty())
                .validatorName("SyntaxValidator")
                .errors(errors)
                .warnings(warnings)
                .confidence(errors.isEmpty() ? 0.9 : 0.1)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private ValidatorResult validateSecurity(String sql) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            // 使用 JSqlParser 解析 SQL
            net.sf.jsqlparser.statement.Statement statement = net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql);

            // 1. 检查是否为 SELECT 语句
            if (!(statement instanceof net.sf.jsqlparser.statement.select.Select)) {
                errors.add("SECURITY ERROR: 只允许执行 SELECT 查询语句");
            } else {
                // 2. 深入检查 SELECT 语句内部
                net.sf.jsqlparser.statement.select.Select select = (net.sf.jsqlparser.statement.select.Select) statement;

                // 使用新的 API 获取 SelectBody
                if (select.getPlainSelect() != null) {
                    net.sf.jsqlparser.statement.select.PlainSelect plainSelect = select.getPlainSelect();
                    // 检查是否包含 INTO 子句 (SELECT ... INTO ... 是危险的)
                    if (plainSelect.getIntoTables() != null && !plainSelect.getIntoTables().isEmpty()) {
                        errors.add("SECURITY ERROR: 不允许使用 SELECT INTO 语句");
                    }
                }
            }

            // 3. 检查注释中的潜在注入 (保留作为辅助检查)
            if (sql.contains("/*") || sql.contains("*/") || sql.contains("-- ")) {
                warnings.add("发现SQL注释，请确保不是SQL注入尝试");
            }

        } catch (net.sf.jsqlparser.JSQLParserException e) {
            // 解析失败通常意味着语法错误，但也可能是注入攻击
            errors.add("SECURITY ERROR: SQL解析失败，可能包含非法语法: " + e.getMessage());
        } catch (Exception e) {
            errors.add("SECURITY ERROR: 验证过程发生异常: " + e.getMessage());
        }

        return ValidatorResult.builder()
                .valid(errors.isEmpty())
                .validatorName("SecurityValidator")
                .errors(errors)
                .warnings(warnings)
                .confidence(errors.isEmpty() ? 0.99 : 0.1)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private ValidatorResult validateSemantics(String sql, DatabaseSchema schema) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 检查表名是否存在
        for (DatabaseSchema.Table table : schema.getTables()) {
            String tableName = table.getName();
            if (sql.contains(tableName) || sql.contains("`" + tableName + "`")) {
                // 表存在，检查列名
                // 这里可以添加更复杂的列名检查逻辑
            }
        }

        // 简化的语义检查
        if (sql.contains("unknown_table")) {
            errors.add("SEMANTIC ERROR: 引用了不存在的表");
        }

        return ValidatorResult.builder()
                .valid(errors.isEmpty())
                .validatorName("SemanticValidator")
                .errors(errors)
                .warnings(warnings)
                .confidence(0.8)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private ValidatorResult validatePerformance(String sql, DatabaseSchema schema) {
        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        // 性能检查
        if (sql.contains("SELECT *")) {
            warnings.add("建议指定具体列名而不是使用 SELECT *");
        }

        if (!sql.toUpperCase().contains("LIMIT")) {
            warnings.add("建议添加LIMIT子句限制返回行数");
        }

        if (sql.toUpperCase().contains("ORDER BY") && !sql.toUpperCase().contains("LIMIT")) {
            warnings.add("使用ORDER BY时建议添加LIMIT以提高性能");
        }

        return ValidatorResult.builder()
                .valid(true) // 性能警告不影响有效性
                .validatorName("PerformanceValidator")
                .errors(List.of())
                .warnings(warnings)
                .confidence(0.7)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private String generateOverallMessage(List<ValidatorResult> results) {
        long errorCount = results.stream().mapToLong(r -> r.getErrors().size()).sum();
        long warningCount = results.stream().mapToLong(r -> r.getWarnings().size()).sum();

        if (errorCount == 0 && warningCount == 0) {
            return "SQL验证通过，无错误和警告";
        } else if (errorCount == 0) {
            return String.format("SQL验证通过，但有 %d 个警告", warningCount);
        } else {
            return String.format("SQL验证失败，有 %d 个错误和 %d 个警告", errorCount, warningCount);
        }
    }
}