package com.kami.springai.datasource.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 数据源配置实体类
 */
@Data
public class DataSourceConfig {
    private String id;
    private String name;
    private String type; // mysql, postgresql, oracle等
    private String host;
    private int port;
    private String database;
    private String username;

    private String password;
    
    private int minimumIdle = 5;
    private int maximumPoolSize = 20;
    private long connectionTimeout = 30000L;
    private boolean sslEnabled = false;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;
    private boolean isDefault = false;
    private boolean active = true;

    private String driverClassName; // 添加缺失的字段
    
    public DataSourceConfig() {
        this.id = "ds-" + UUID.randomUUID().toString().substring(0, 8);
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * 构建JDBC URL
     */
    public String buildJdbcUrl() {
        StringBuilder url = new StringBuilder();
        
        switch (type.toLowerCase()) {
            case "mysql":
                url.append("jdbc:mysql://").append(host).append(":").append(port)
                    .append("/").append(database)
                    .append("?useSSL=").append(sslEnabled)
                    .append("&serverTimezone=UTC")
                    .append("&characterEncoding=utf8")
                    .append("&allowPublicKeyRetrieval=true");
                break;
            case "postgresql":
                url.append("jdbc:postgresql://").append(host).append(":").append(port)
                    .append("/").append(database)
                    .append("?sslmode=").append(sslEnabled ? "require" : "disable");
                break;
            case "oracle":
                url.append("jdbc:oracle:thin:@").append(host).append(":").append(port)
                    .append(":").append(database);
                break;
            case "h2":
                url.append("jdbc:h2:mem:").append(database)
                    .append(";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false");
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
        
        return url.toString();
    }
    
    /**
     * 获取驱动类名
     */
    public String getDriverClassName() {
        switch (type.toLowerCase()) {
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "postgresql":
                return "org.postgresql.Driver";
            case "oracle":
                return "oracle.jdbc.OracleDriver";
            case "h2":
                return "org.h2.Driver";
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }
}