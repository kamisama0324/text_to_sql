package com.kami.springai.datasource.config;

import com.kami.springai.datasource.filter.DataSourceContextFilter;
import com.kami.springai.datasource.service.DynamicDataSourceManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * 数据源自动配置类
 * 负责自动配置数据源相关组件
 */
@Slf4j
@Configuration
@Import(DynamicDataSourceConfig.class)
@EnableConfigurationProperties(DataSourceAutoConfiguration.DataSourceProperties.class)
@RequiredArgsConstructor
public class DataSourceAutoConfiguration {

    /**
     * 数据源配置属性类
     * 支持spring.datasource前缀配置
     */
    @ConfigurationProperties(prefix = "spring.datasource")
    public static class DataSourceProperties {
        private String driverClassName;
        private String url;
        private String username;
        private String password;
        private int maxPoolSize = 10;
        private int minIdle = 5;
        private long connectionTimeout = 30000;
        private long maxLifetime = 1800000;
        
        // Getters and setters
        public String getDriverClassName() {
            return driverClassName;
        }
        
        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
        
        public int getMinIdle() {
            return minIdle;
        }
        
        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }
        
        public long getConnectionTimeout() {
            return connectionTimeout;
        }
        
        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        public long getMaxLifetime() {
            return maxLifetime;
        }
        
        public void setMaxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }
    }

    /**
     * 数据源上下文过滤器
     */
    @Bean
    @ConditionalOnClass(DataSource.class)
    public DataSourceContextFilter dataSourceContextFilter() {
        log.info("注册数据源上下文过滤器");
        return new DataSourceContextFilter();
    }

    /**
     * 初始化动态数据源管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public DynamicDataSourceManager dynamicDataSourceManager(DataSourceProperties dataSourceProperties) {
        log.info("初始化动态数据源管理器");
        DynamicDataSourceManager manager = new DynamicDataSourceManager();
        return manager;
    }

    @PostConstruct
    public void init() {
        log.info("数据源自动配置初始化完成");
    }
}