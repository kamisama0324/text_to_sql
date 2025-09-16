package com.kami.springai.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpResponse {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("result")
    private String result;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("mcp_type")
    private String mcpType;
    
    @JsonProperty("execution_time_ms")
    private long executionTimeMs;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("thread_info")
    private String threadInfo; // 显示使用的线程信息
    
    public static McpResponse success(String result, String mcpType, long executionTime) {
        return McpResponse.builder()
                .success(true)
                .result(result)
                .mcpType(mcpType)
                .executionTimeMs(executionTime)
                .timestamp(LocalDateTime.now())
                .threadInfo(Thread.currentThread().toString())
                .build();
    }
    
    public static McpResponse error(String error, String mcpType, long executionTime) {
        return McpResponse.builder()
                .success(false)
                .error(error)
                .mcpType(mcpType)
                .executionTimeMs(executionTime)
                .timestamp(LocalDateTime.now())
                .threadInfo(Thread.currentThread().toString())
                .build();
    }
}