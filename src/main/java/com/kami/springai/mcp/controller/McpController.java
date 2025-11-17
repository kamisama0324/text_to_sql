package com.kami.springai.mcp.controller;

import com.kami.springai.mcp.model.McpRequest;
import com.kami.springai.mcp.model.McpResponse;
import com.kami.springai.mcp.service.McpService;
import com.kami.springai.mcp.server.SimpleMcpServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpService mcpService;
    private final SimpleMcpServer simpleMcpServer;

    /**
     * 执行文件系统MCP任务
     */
    @PostMapping("/filesystem")
    public ResponseEntity<McpResponse> executeFilesystemTask(@RequestBody McpRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (request.isAsync()) {
                // 异步执行
                CompletableFuture<String> future = mcpService.executeFilesystemTask(request.getPrompt());
                return ResponseEntity.ok(McpResponse.success(
                    "任务已异步启动", 
                    "filesystem", 
                    System.currentTimeMillis() - startTime
                ));
            } else {
                // 同步执行
                CompletableFuture<String> future = mcpService.executeFilesystemTask(request.getPrompt());
                String result = future.get();
                return ResponseEntity.ok(McpResponse.success(
                    result, 
                    "filesystem", 
                    System.currentTimeMillis() - startTime
                ));
            }
        } catch (Exception e) {
            log.error("文件系统MCP任务执行失败", e);
            return ResponseEntity.ok(McpResponse.error(
                e.getMessage(), 
                "filesystem", 
                System.currentTimeMillis() - startTime
            ));
        }
    }

    /**
     * 执行GitHub MCP任务
     */
    @PostMapping("/github")
    public ResponseEntity<McpResponse> executeGithubTask(@RequestBody McpRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (request.isAsync()) {
                // 异步执行
                CompletableFuture<String> future = mcpService.executeGithubTask(request.getPrompt());
                return ResponseEntity.ok(McpResponse.success(
                    "任务已异步启动", 
                    "github", 
                    System.currentTimeMillis() - startTime
                ));
            } else {
                // 同步执行
                CompletableFuture<String> future = mcpService.executeGithubTask(request.getPrompt());
                String result = future.get();
                return ResponseEntity.ok(McpResponse.success(
                    result, 
                    "github", 
                    System.currentTimeMillis() - startTime
                ));
            }
        } catch (Exception e) {
            log.error("GitHub MCP任务执行失败", e);
            return ResponseEntity.ok(McpResponse.error(
                e.getMessage(), 
                "github", 
                System.currentTimeMillis() - startTime
            ));
        }
    }

    /**
     * 使用结构化并发执行多个任务
     */
    @PostMapping("/multiple")
    public ResponseEntity<McpResponse> executeMultipleTasks(
            @RequestParam String filesystemPrompt,
            @RequestParam String githubPrompt) {
        long startTime = System.currentTimeMillis();
        
        try {
            String result = mcpService.executeMultipleTasks(filesystemPrompt, githubPrompt);
            return ResponseEntity.ok(McpResponse.success(
                result, 
                "multiple", 
                System.currentTimeMillis() - startTime
            ));
        } catch (Exception e) {
            log.error("多任务执行失败", e);
            return ResponseEntity.ok(McpResponse.error(
                e.getMessage(), 
                "multiple", 
                System.currentTimeMillis() - startTime
            ));
        }
    }

    /**
     * 获取MCP连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<McpResponse> getStatus() {
        long startTime = System.currentTimeMillis();
        
        try {
            String status = mcpService.getConnectionStatus();
            return ResponseEntity.ok(McpResponse.success(
                status, 
                "status", 
                System.currentTimeMillis() - startTime
            ));
        } catch (Exception e) {
            log.error("获取状态失败", e);
            return ResponseEntity.ok(McpResponse.error(
                e.getMessage(), 
                "status", 
                System.currentTimeMillis() - startTime
            ));
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Spring AI MCP服务运行正常");
    }

    // ==================== Text2SQL MCP端点 ====================

    /**
     * 获取MCP服务器信息
     */
    @GetMapping("/server-info")
    public ResponseEntity<SimpleMcpServer.ServerInfo> getServerInfo() {
        try {
            SimpleMcpServer.ServerInfo info = simpleMcpServer.getServerInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("获取服务器信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取可用工具列表
     */
    @GetMapping("/tools")
    public ResponseEntity<SimpleMcpServer.ToolsList> getAvailableTools() {
        try {
            SimpleMcpServer.ToolsList tools = simpleMcpServer.getAvailableTools();
            return ResponseEntity.ok(tools);
        } catch (Exception e) {
            log.error("获取工具列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 执行工具调用
     */
    @PostMapping("/tool/{toolName}")
    public ResponseEntity<SimpleMcpServer.ToolResult> executeTool(
            @PathVariable String toolName,
            @RequestBody java.util.Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("执行工具: {} 参数: {}", toolName, parameters);
            SimpleMcpServer.ToolResult result = simpleMcpServer.executeTool(toolName, parameters);
            
            log.info("工具执行完成，耗时: {}ms", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("工具执行失败: {}", e.getMessage(), e);
            return ResponseEntity.ok(SimpleMcpServer.ToolResult.error("工具执行失败: " + e.getMessage()));
        }
    }

    /**
     * Text2SQL转换
     */
    @PostMapping("/text2sql")
    public ResponseEntity<SimpleMcpServer.ToolResult> convertTextToSql(@RequestBody McpRequest request) {
        try {
            // 设置数据源上下文
            if (request.getDataSourceId() != null) {
                com.kami.springai.datasource.service.DataSourceContextHolder.setDataSourceId(request.getDataSourceId());
                log.info("设置数据源上下文: {}", request.getDataSourceId());
            } else {
                log.warn("Text2SQL请求中未提供数据源ID");
            }

            java.util.Map<String, Object> parameters = new java.util.HashMap<>();
            parameters.put("query", request.getPrompt());
            parameters.put("context", request.getContext());

            SimpleMcpServer.ToolResult result = simpleMcpServer.executeTool("text_to_sql", parameters);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Text2SQL转换失败", e);
            return ResponseEntity.ok(SimpleMcpServer.ToolResult.error("转换失败: " + e.getMessage()));
        } finally {
            // 清理数据源上下文，避免影响其他请求
            com.kami.springai.datasource.service.DataSourceContextHolder.clear();
        }
    }

    /**
     * SQL执行
     */
    @PostMapping("/execute-sql")
    public ResponseEntity<SimpleMcpServer.ToolResult> executeSql(@RequestBody java.util.Map<String, String> request) {
        try {
            java.util.Map<String, Object> parameters = new java.util.HashMap<>();
            parameters.put("sql", request.get("sql"));
            
            SimpleMcpServer.ToolResult result = simpleMcpServer.executeTool("execute_sql", parameters);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("SQL执行失败", e);
            return ResponseEntity.ok(SimpleMcpServer.ToolResult.error("执行失败: " + e.getMessage()));
        }
    }

    /**
     * 获取数据库结构
     */
    @GetMapping("/database-schema")
    public ResponseEntity<SimpleMcpServer.ToolResult> getDatabaseSchema(
            @RequestParam(required = false) String tableName) {
        try {
            java.util.Map<String, Object> parameters = new java.util.HashMap<>();
            if (tableName != null) {
                parameters.put("table_name", tableName);
            }
            
            SimpleMcpServer.ToolResult result = simpleMcpServer.executeTool("get_database_schema", parameters);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取数据库结构失败", e);
            return ResponseEntity.ok(SimpleMcpServer.ToolResult.error("获取失败: " + e.getMessage()));
        }
    }

    /**
     * SQL解释
     */
    @PostMapping("/explain-sql")
    public ResponseEntity<SimpleMcpServer.ToolResult> explainSql(@RequestBody java.util.Map<String, String> request) {
        try {
            java.util.Map<String, Object> parameters = new java.util.HashMap<>();
            parameters.put("sql", request.get("sql"));
            parameters.put("context", request.get("context"));
            
            SimpleMcpServer.ToolResult result = simpleMcpServer.executeTool("explain_sql", parameters);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("SQL解释失败", e);
            return ResponseEntity.ok(SimpleMcpServer.ToolResult.error("解释失败: " + e.getMessage()));
        }
    }

    /**
     * 用户反馈接口
     */
    @PostMapping("/user-feedback")
    public ResponseEntity<SimpleMcpServer.ToolResult> submitUserFeedback(
            @RequestParam String userQuery,
            @RequestParam String generatedSql,
            @RequestParam String isCorrect,
            @RequestParam(required = false) String correctedSql) {
        try {
            log.info("收到用户反馈 - 查询: {}, SQL是否正确: {}", userQuery, isCorrect);
            
            // 构建反馈消息
            StringBuilder feedbackMessage = new StringBuilder();
            feedbackMessage.append("用户查询: ").append(userQuery).append("\n");
            feedbackMessage.append("生成的SQL: ").append(generatedSql).append("\n");
            feedbackMessage.append("是否正确: ").append(isCorrect).append("\n");
            
            if (correctedSql != null && !correctedSql.isEmpty()) {
                feedbackMessage.append("修正后的SQL: ").append(correctedSql).append("\n");
            }
            
            // 这里可以将反馈保存到数据库或日志文件中
            log.info("用户反馈详情:\n{}", feedbackMessage.toString());
            
            // 返回成功响应
            return ResponseEntity.ok(SimpleMcpServer.ToolResult.success(
                "反馈已成功记录，感谢您帮助我们改进！"
            ));
            
        } catch (Exception e) {
            log.error("处理用户反馈失败", e);
            return ResponseEntity.ok(SimpleMcpServer.ToolResult.error("反馈提交失败: " + e.getMessage()));
        }
    }
}