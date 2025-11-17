package com.kami.springai.datasource;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 数据源模块主配置类
 * 用于扫描和初始化数据源相关组件
 */
@Configuration
@ComponentScan(basePackages = {
        "com.kami.springai.datasource.config",
        "com.kami.springai.datasource.service",
        "com.kami.springai.datasource.controller",
        "com.kami.springai.datasource.filter"
})
public class DataSourceConfiguration {
    // 配置类，用于组件扫描
}