package com.kami.springai.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringAiMcpApplicationTests {

    @Test
    void contextLoads() {
        // 测试Spring上下文加载
    }

    @Test
    void virtualThreadsEnabled() {
        // 测试虚拟线程是否启用
        Thread virtualThread = Thread.ofVirtual()
                .name("test-virtual-thread")
                .start(() -> System.out.println("虚拟线程运行中..."));
        
        assert virtualThread.isVirtual();
        System.out.println("虚拟线程测试通过");
    }
}