package com.kami.springai.mcp.config;

// import com.kami.springai.mcp.server.Text2SqlMcpServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.ai.mcp.server.transport.ServerTransport;
// import org.springframework.ai.mcp.server.transport.StdioServerTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Text2SQL MCP服务器配置类
 * 
 * 配置并启动Text2SQL MCP服务器，提供标准的MCP协议接口。
 * 
 * @author Text2SQL-MCP
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class Text2SqlMcpServerConfig {

    /**
     * 临时配置类 - 等待Spring AI MCP完全稳定
     */
    @Bean
    public String mcpServerStatus() {
        log.info("Text2SQL MCP服务器配置完成 - 使用简化版本");
        return "SimpleMcpServer Ready";
    }
}