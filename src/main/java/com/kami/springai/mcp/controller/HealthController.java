package com.kami.springai.mcp.controller;

import com.kami.springai.mcp.server.SimpleMcpServer;
// import com.kami.springai.common.cache.SchemaCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器
 * 
 * 提供基本的系统状态检查和Text2SQL功能测试接口
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final SimpleMcpServer simpleMcpServer;

    /**
     * 基本健康检查
     */
    @GetMapping
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "Text2SQL MCP Server",
            "version", "1.0.0",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * MCP服务器状态
     */
    @GetMapping("/mcp")
    public Map<String, Object> mcpHealth() {
        try {
            SimpleMcpServer.ServerInfo serverInfo = simpleMcpServer.getServerInfo();
            SimpleMcpServer.ToolsList tools = simpleMcpServer.getAvailableTools();
            
            return Map.of(
                "server_info", serverInfo,
                "available_tools", tools.getTools().size(),
                "status", "UP"
            );
        } catch (Exception e) {
            log.error("MCP健康检查失败", e);
            return Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            );
        }
    }

    /**
     * 测试Text2SQL转换
     */
    @GetMapping("/test-text2sql")
    public Map<String, Object> testText2Sql() {
        try {
            String testQuery = "查询所有用户信息";
            java.util.Map<String, Object> parameters = new java.util.HashMap<>();
            parameters.put("query", testQuery);
            
            SimpleMcpServer.ToolResult result = simpleMcpServer.executeTool("text_to_sql", parameters);
            
            return Map.of(
                "test_query", testQuery,
                "result", result,
                "status", result.isSuccess() ? "SUCCESS" : "FAILED"
            );
        } catch (Exception e) {
            log.error("Text2SQL测试失败", e);
            return Map.of(
                "status", "FAILED",
                "error", e.getMessage()
            );
        }
    }
}