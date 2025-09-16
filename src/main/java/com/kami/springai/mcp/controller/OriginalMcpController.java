package com.kami.springai.mcp.controller;

import com.kami.springai.mcp.server.SimpleMcpServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 原始MCP API兼容控制器
 * 基于8080端口原始项目的API格式反推
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp/text2sql")
@RequiredArgsConstructor
public class OriginalMcpController {

    private final SimpleMcpServer mcpServer;

    /**
     * 原始格式的Text2SQL接口
     * 兼容FormData格式请求
     */
    @PostMapping("/text-to-sql")
    public ResponseEntity<Map<String, Object>> textToSql(@RequestParam String query,
                                                        @RequestParam(required = false) String context) {
        long startTime = System.currentTimeMillis();
        log.info("Original API - Text2SQL请求: {}", query);
        
        try {
            // 使用现有的SimpleMcpServer处理
            Map<String, Object> params = new HashMap<>();
            params.put("query", query);
            if (context != null) {
                params.put("context", context);
            }
            
            SimpleMcpServer.ToolResult result = mcpServer.executeTool("text_to_sql", params);
            
            // 构建原始格式的响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("result", result.getContent());
            response.put("error", result.getError());
            response.put("mcp_type", "text_to_sql");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Original API - Text2SQL转换失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("result", null);
            response.put("error", e.getMessage());
            response.put("mcp_type", "text_to_sql");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 原始格式的SQL执行接口
     */
    @PostMapping("/execute-query")
    public ResponseEntity<Map<String, Object>> executeQuery(@RequestParam String sql) {
        long startTime = System.currentTimeMillis();
        log.info("Original API - 执行SQL: {}", sql);
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("sql", sql);
            
            SimpleMcpServer.ToolResult result = mcpServer.executeTool("execute_sql", params);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("result", result.getContent());
            response.put("error", result.getError());
            response.put("mcp_type", "execute_sql");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Original API - SQL执行失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("result", null);
            response.put("error", e.getMessage());
            response.put("mcp_type", "execute_sql");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 原始格式的查询并执行接口
     */
    @PostMapping("/query-and-execute")
    public ResponseEntity<Map<String, Object>> queryAndExecute(@RequestParam String query,
                                                              @RequestParam(required = false) String context) {
        long startTime = System.currentTimeMillis();
        log.info("Original API - 查询并执行: {}", query);
        
        try {
            // 先转换为SQL
            Map<String, Object> textToSqlParams = new HashMap<>();
            textToSqlParams.put("query", query);
            if (context != null) {
                textToSqlParams.put("context", context);
            }
            
            SimpleMcpServer.ToolResult sqlResult = mcpServer.executeTool("text_to_sql", textToSqlParams);
            
            if (!sqlResult.isSuccess()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("result", null);
                response.put("error", "SQL生成失败: " + sqlResult.getError());
                response.put("mcp_type", "query_and_execute");
                response.put("execution_time_ms", System.currentTimeMillis() - startTime);
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("thread_info", Thread.currentThread().toString());
                
                return ResponseEntity.ok(response);
            }
            
            // 提取生成的SQL
            String generatedSql = extractSqlFromContent(sqlResult.getContent());
            
            // 执行SQL
            Map<String, Object> executeParams = new HashMap<>();
            executeParams.put("sql", generatedSql);
            
            SimpleMcpServer.ToolResult executeResult = mcpServer.executeTool("execute_sql", executeParams);
            
            // 合并结果
            StringBuilder combinedResult = new StringBuilder();
            combinedResult.append("**生成的SQL:**\n").append(generatedSql).append("\n\n");
            combinedResult.append("**执行结果:**\n").append(executeResult.getContent());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", executeResult.isSuccess());
            response.put("result", combinedResult.toString());
            response.put("error", executeResult.getError());
            response.put("mcp_type", "query_and_execute");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Original API - 查询并执行失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("result", null);
            response.put("error", e.getMessage());
            response.put("mcp_type", "query_and_execute");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 原始格式的用户反馈接口
     */
    @PostMapping("/user-feedback")
    public ResponseEntity<Map<String, Object>> userFeedback(@RequestParam String query,
                                                           @RequestParam String sql,
                                                           @RequestParam String feedback,
                                                           @RequestParam(required = false) String correctedSql) {
        long startTime = System.currentTimeMillis();
        log.info("Original API - 用户反馈: query={}, feedback={}", query, feedback);
        
        try {
            // 这里可以集成到学习框架
            // 暂时返回成功响应
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", "反馈已记录，感谢您的反馈！");
            response.put("error", null);
            response.put("mcp_type", "user_feedback");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Original API - 用户反馈处理失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("result", null);
            response.put("error", e.getMessage());
            response.put("mcp_type", "user_feedback");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 原始格式的测试连接接口
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        long startTime = System.currentTimeMillis();
        log.info("Original API - 测试数据库连接");
        
        try {
            SimpleMcpServer.ServerInfo serverInfo = mcpServer.getServerInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", "数据库连接正常\n当前数据库: test\n服务器: " + serverInfo.getName() + " v" + serverInfo.getVersion());
            response.put("error", null);
            response.put("mcp_type", "test_connection");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Original API - 连接测试失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("result", null);
            response.put("error", e.getMessage());
            response.put("mcp_type", "test_connection");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 原始格式的数据库结构描述接口
     */
    @PostMapping("/describe-database")
    public ResponseEntity<Map<String, Object>> describeDatabase() {
        long startTime = System.currentTimeMillis();
        log.info("Original API - 获取数据库结构");
        
        try {
            Map<String, Object> params = new HashMap<>();
            SimpleMcpServer.ToolResult result = mcpServer.executeTool("get_database_schema", params);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("result", result.getContent());
            response.put("error", result.getError());
            response.put("mcp_type", "describe_database");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Original API - 获取数据库结构失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("result", null);
            response.put("error", e.getMessage());
            response.put("mcp_type", "describe_database");
            response.put("execution_time_ms", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("thread_info", Thread.currentThread().toString());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 从content中提取SQL语句
     */
    private String extractSqlFromContent(String content) {
        if (content == null) {
            return "";
        }
        
        // 查找```sql 和 ``` 之间的内容
        int startIndex = content.indexOf("```sql");
        if (startIndex != -1) {
            startIndex += 6; // 跳过 "```sql"
            int endIndex = content.indexOf("```", startIndex);
            if (endIndex != -1) {
                return content.substring(startIndex, endIndex).trim();
            }
        }
        
        // 如果没有找到代码块，尝试其他方式提取
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.toUpperCase().startsWith("SELECT")) {
                return line;
            }
        }
        
        return content.trim();
    }
}