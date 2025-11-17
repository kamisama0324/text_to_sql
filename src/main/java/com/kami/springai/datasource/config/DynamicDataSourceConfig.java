package com.kami.springai.datasource.config;

import com.kami.springai.datasource.service.DynamicDataSourceManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * 动态数据源配置类
 * 将DynamicDataSourceManager注册为Spring的主要数据源
 */
@Configuration
public class DynamicDataSourceConfig {
    
    /**
     * 注册DynamicDataSourceManager为主要数据源
     */
    @Bean
    @Primary
    public DataSource dynamicDataSource(DynamicDataSourceManager dataSourceManager) {
        // 返回DynamicDataSourceManager作为主数据源
        return dataSourceManager;
    }
    
    /**
     * 创建默认数据源（当没有指定数据源时使用）
     * 使用嵌入式H2数据库以确保Spring Data JDBC能够正常初始化
     */
    @Bean
    @ConditionalOnMissingBean(name = "defaultDataSource")
    public DataSource defaultDataSource() {
        // 创建一个简单的嵌入式H2数据库，确保即使没有实际数据源也能正常启动
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}