package com.kami.springai.text2sql.service;

import com.kami.springai.datasource.service.DynamicDataSourceManager;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL执行服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecutionService {

    private final DynamicDataSourceManager dynamicDataSourceManager;

    /**
     * 执行查询
     */
    public QueryResult executeQuery(String sql) {
        log.info("执行SQL查询: {}", sql);
        
        // 验证SQL安全性
        validateSqlSafety(sql);
        
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = getCurrentDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            
            // 获取列信息
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columns = new ArrayList<>();
            
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnName(i));
            }
            
            // 获取数据行
            List<Map<String, Object>> rows = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                rows.add(row);
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            return QueryResult.builder()
                    .columns(columns)
                    .rows(rows)
                    .totalRows(rows.size())
                    .executionTime(executionTime)
                    .success(true)
                    .build();
            
        } catch (Exception e) {
            log.error("SQL执行失败: {}", e.getMessage(), e);
            long executionTime = System.currentTimeMillis() - startTime;
            
            return QueryResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .executionTime(executionTime)
                    .build();
        }
    }
    
    /**
     * 获取当前数据源
     */
    private DataSource getCurrentDataSource() {
        return dynamicDataSourceManager.getCurrentDataSource();
    }

    /**
     * 验证SQL安全性
     */
    private void validateSqlSafety(String sql) {
        String upperSql = sql.toUpperCase().trim();
        
        // 检查是否只包含SELECT语句
        if (!upperSql.startsWith("SELECT")) {
            throw new RuntimeException("只允许SELECT查询，检测到非法操作");
        }
        
        // 检查是否包含危险关键字
        String[] dangerousKeywords = {
            "DELETE", "UPDATE", "INSERT", "DROP", "ALTER", "CREATE", 
            "TRUNCATE", "REPLACE", "MERGE", "CALL", "EXEC"
        };
        
        for (String keyword : dangerousKeywords) {
            if (upperSql.matches(".*\\b" + keyword + "\\b.*")) {
                throw new RuntimeException("SQL包含不允许的操作: " + keyword);
            }
        }
        
        // 检查SQL语句长度
        if (sql.length() > 3000) {
            throw new RuntimeException("SQL语句过长，可能存在安全风险");
        }
        
        // 检查是否包含多个SQL语句
        if (sql.trim().split(";").length > 2) {
            throw new RuntimeException("不允许执行多个SQL语句");
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class QueryResult {
        private List<String> columns;
        private List<Map<String, Object>> rows;
        private int totalRows;
        private long executionTime;
        private boolean success;
        private String errorMessage;
    }
}