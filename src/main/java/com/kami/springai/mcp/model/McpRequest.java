package com.kami.springai.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpRequest {
    
    @JsonProperty("prompt")
    private String prompt;
    
    @JsonProperty("mcp_type")
    private String mcpType; // filesystem, github, etc.
    
    @JsonProperty("async")
    private boolean async = false;
    
    @JsonProperty("timeout")
    private Long timeout = 30000L; // 默认30秒超时
    
    @JsonProperty("context")
    private String context; // 额外上下文信息
}