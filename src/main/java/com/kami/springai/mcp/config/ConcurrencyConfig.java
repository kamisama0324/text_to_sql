package com.kami.springai.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class ConcurrencyConfig {

    /**
     * 配置虚拟线程执行器 - 利用Java 21+的虚拟线程特性
     */
    @Bean("virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name("virtual-", 0)
                        .factory()
        );
    }

    /**
     * 传统线程池执行器 - 用于CPU密集型任务
     */
    @Bean("cpuIntensiveExecutor")
    public Executor cpuIntensiveExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cpu-intensive-");
        executor.initialize();
        return executor;
    }

    /**
     * MCP专用执行器 - 使用虚拟线程处理MCP通信
     */
    @Bean("mcpExecutor") 
    public Executor mcpExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual()
                        .name("mcp-", 0)
                        .factory()
        );
    }
}