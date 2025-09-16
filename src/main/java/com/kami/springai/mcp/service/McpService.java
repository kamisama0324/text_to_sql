package com.kami.springai.mcp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class McpService {

    // TODO: 等MCP依赖正确导入后，重新配置客户端
    /*
    private final McpClient filesystemMcpClient;
    private final McpClient githubMcpClient;
    private final ChatClient chatClient;

    public McpService(@Qualifier("filesystemMcpClient") McpClient filesystemMcpClient,
                     @Qualifier("githubMcpClient") McpClient githubMcpClient,
                     ChatClient.Builder chatClientBuilder) {
        this.filesystemMcpClient = filesystemMcpClient;
        this.githubMcpClient = githubMcpClient;
        this.chatClient = chatClientBuilder.build();
    }
    */

    /**
     * 测试方法 - 使用虚拟线程
     */
    @Async("virtualThreadExecutor")
    public CompletableFuture<String> executeFilesystemTask(String prompt) {
        try {
            log.info("执行文件系统任务: {}", prompt);
            log.info("当前线程信息: {}", Thread.currentThread());
            
            // 使用CompletableFuture.delayedExecutor代替Thread.sleep
            // 非阻塞延迟操作
            return CompletableFuture.supplyAsync(() -> {
                String response = "文件系统任务完成: " + prompt + " (使用虚拟线程: " + Thread.currentThread().isVirtual() + ")";
                log.info("文件系统任务完成");
                return response;
            }, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.error("文件系统任务执行失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 测试方法 - GitHub任务
     */
    @Async("virtualThreadExecutor")
    public CompletableFuture<String> executeGithubTask(String prompt) {
        try {
            log.info("执行GitHub任务: {}", prompt);
            log.info("当前线程信息: {}", Thread.currentThread());
            
            // 使用CompletableFuture.delayedExecutor代替Thread.sleep
            // 非阻塞延迟操作
            return CompletableFuture.supplyAsync(() -> {
                String response = "GitHub任务完成: " + prompt + " (使用虚拟线程: " + Thread.currentThread().isVirtual() + ")";
                log.info("GitHub任务完成");
                return response;
            }, CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.error("GitHub任务执行失败", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Java 24结构化并发测试
     */
    public String executeMultipleTasks(String filesystemPrompt, String githubPrompt) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            log.info("开始结构化并发任务");
            
            // 启动文件系统任务 - 使用CompletableFuture代替阻塞sleep
            var filesystemTask = scope.fork(() -> {
                try {
                    // 使用CompletableFuture的超时等待替代Thread.sleep
                    CompletableFuture<String> delayed = CompletableFuture.supplyAsync(
                        () -> "文件系统任务: " + filesystemPrompt + " (虚拟线程: " + Thread.currentThread().isVirtual() + ")",
                        CompletableFuture.delayedExecutor(800, TimeUnit.MILLISECONDS)
                    );
                    return delayed.get(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("文件系统任务失败", e);
                    throw new RuntimeException(e);
                }
            });

            // 启动GitHub任务 - 使用CompletableFuture代替阻塞sleep
            var githubTask = scope.fork(() -> {
                try {
                    // 使用CompletableFuture的超时等待替代Thread.sleep
                    CompletableFuture<String> delayed = CompletableFuture.supplyAsync(
                        () -> "GitHub任务: " + githubPrompt + " (虚拟线程: " + Thread.currentThread().isVirtual() + ")",
                        CompletableFuture.delayedExecutor(600, TimeUnit.MILLISECONDS)
                    );
                    return delayed.get(1, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("GitHub任务失败", e);
                    throw new RuntimeException(e);
                }
            });

            // 等待所有任务完成
            scope.join();           // Join both subtasks
            scope.throwIfFailed();  // Propagate exception if any subtask failed

            // 收集结果
            StringBuilder result = new StringBuilder();
            result.append("结构化并发结果:\n");
            result.append("- ").append(filesystemTask.get()).append("\n");
            result.append("- ").append(githubTask.get());

            return result.toString();
            
        } catch (Exception e) {
            log.error("结构化并发任务执行失败", e);
            throw new RuntimeException("多任务执行失败", e);
        }
    }

    /**
     * 检查服务状态
     */
    public String getConnectionStatus() {
        StringBuilder status = new StringBuilder();
        
        status.append("Spring AI MCP服务状态:\n");
        status.append("- 虚拟线程支持: ").append(Thread.ofVirtual().factory() != null ? "✅" : "❌").append("\n");
        status.append("- Java版本: ").append(System.getProperty("java.version")).append("\n");
        status.append("- 当前线程: ").append(Thread.currentThread()).append("\n");
        status.append("- 服务状态: 正常运行");
        
        return status.toString();
    }
}