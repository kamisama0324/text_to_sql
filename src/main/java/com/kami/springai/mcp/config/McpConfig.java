package com.kami.springai.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.Executors;

@Configuration
public class McpConfig {

    // TODO: 等Spring AI MCP依赖正确导入后，重新配置MCP客户端
    /*
    @Bean
    @Primary
    public McpClient filesystemMcpClient() {
        McpTransport transport = StdioMcpTransport.builder()
                .command(List.of("npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp"))
                .executor(Executors.newVirtualThreadPerTaskExecutor()) // Java 21+ 虚拟线程
                .build();

        return McpClient.builder()
                .transport(transport)
                .build();
    }

    @Bean
    public McpClient githubMcpClient() {
        String githubToken = System.getenv("GITHUB_TOKEN");
        if (githubToken == null || githubToken.isEmpty()) {
            return null; // GitHub token未配置时返回null
        }

        McpTransport transport = StdioMcpTransport.builder()
                .command(List.of("npx", "-y", "@modelcontextprotocol/server-github"))
                .environment("GITHUB_PERSONAL_ACCESS_TOKEN", githubToken)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        return McpClient.builder()
                .transport(transport)
                .build();
    }
    */
}