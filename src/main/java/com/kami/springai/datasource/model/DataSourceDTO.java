package com.kami.springai.datasource.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源DTO类，用于API响应，不包含密码信息
 */
@Data
public class DataSourceDTO {
    private String id;
    private String name;
    private String type;
    private String host;
    private int port;
    private String database;
    private String username;
    private int minimumIdle;
    private int maximumPoolSize;
    private boolean sslEnabled;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;
    private boolean isDefault;
    private boolean active;
    private DataSourceStatus status;
    
    /**
     * 从配置实体转换为DTO
     */
    public static DataSourceDTO fromConfig(DataSourceConfig config, DataSourceStatus status) {
        DataSourceDTO dto = new DataSourceDTO();
        dto.setId(config.getId());
        dto.setName(config.getName());
        dto.setType(config.getType());
        dto.setHost(config.getHost());
        dto.setPort(config.getPort());
        dto.setDatabase(config.getDatabase());
        dto.setUsername(config.getUsername());
        dto.setMinimumIdle(config.getMinimumIdle());
        dto.setMaximumPoolSize(config.getMaximumPoolSize());
        dto.setSslEnabled(config.isSslEnabled());
        dto.setDescription(config.getDescription());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setCreatedBy(config.getCreatedBy());
        dto.setDefault(config.isDefault());
        dto.setActive(config.isActive());
        dto.setStatus(status);
        return dto;
    }
    
    /**
     * 获取数据源连接字符串（用于显示）
     */
    public String getConnectionUrl() {
        return String.format("%s://%s:%d/%s", type.toLowerCase(), host, port, database);
    }
}