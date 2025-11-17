package com.kami.springai.datasource.service;

import com.kami.springai.datasource.config.DataSourceAutoConfiguration;
import com.kami.springai.datasource.model.DataSourceConfig;
import com.kami.springai.datasource.model.DataSourceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * 动态数据源管理器
 * 核心组件，负责管理多个数据源的创建、获取和销毁
 */
@Slf4j
@Component
public class DynamicDataSourceManager implements DataSource {
    
    @Autowired
    private DataSourceAutoConfiguration.DataSourceProperties dataSourceProperties;
    
    @Autowired
    private DataSourceRepository dataSourceRepository;
    
    // 数据源映射
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    // 数据源配置缓存（用于状态检查）
    private final Map<String, DataSourceConfig> dataSourceConfigCache = new ConcurrentHashMap<>();

    // 当前活跃数据源ID
    private final AtomicReference<String> activeDataSourceId = new AtomicReference<>();

    // 默认数据源
    private DataSource defaultDataSource;
    
    /**
     * 初始化数据源管理器
     */
    @PostConstruct
    public void initialize() {
        log.info("正在初始化动态数据源管理器");

        try {
            // 加载保存的数据源配置
            List<DataSourceConfig> configs = dataSourceRepository.findAll();
            log.info("加载到 {} 个数据源配置", configs.size());

            // 创建并激活数据源
            for (DataSourceConfig config : configs) {
                if (config.isActive()) {
                    createDataSource(config);
                    log.info("成功初始化数据源: {}", config.getId());
                }
            }

        } catch (Exception e) {
            log.error("初始化数据源管理器失败", e);
        }
    }

    /**
     * 应用启动后的备用初始化方法
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("应用启动完成，检查数据源初始化状态");

        // 检查是否已有数据源被加载
        if (dataSourceMap.isEmpty()) {
            log.info("运行时数据源映射为空，重新尝试初始化");
            initialize();
        } else {
            log.info("已加载 {} 个数据源到运行时映射", dataSourceMap.size());
        }
    }
    
    /**
     * 创建数据源
     */
    public String createDataSource(DataSourceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("数据源配置不能为空");
        }
        
        // 如果ID为null，生成默认ID
        if (config.getId() == null) {
            String defaultId = "ds-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            config.setId(defaultId);
            log.info("自动生成数据源ID: {}", defaultId);
        }
        
        log.info("创建数据源: {}, ID: {}, 类型: {}", config.getName(), config.getId(), config.getType());
        log.info("数据源配置详情: 主机={}, 端口={}, 数据库={}, 用户名={}, 密码长度={}", 
                 config.getHost(), config.getPort(), config.getDatabase(), 
                 config.getUsername(), config.getPassword() != null ? config.getPassword().length() : 0);
        log.info("连接池参数: minimumIdle={}, maximumPoolSize={}, connectionTimeout={}",
                 config.getMinimumIdle(), config.getMaximumPoolSize(), config.getConnectionTimeout());
        
        try {
            // 创建HikariCP数据源但不立即测试连接
            com.zaxxer.hikari.HikariDataSource hikariDataSource = new com.zaxxer.hikari.HikariDataSource();

            // 设置连接参数
            String jdbcUrl = config.buildJdbcUrl();
            log.info("构建的JDBC URL: {}", jdbcUrl);
            hikariDataSource.setJdbcUrl(jdbcUrl);
            hikariDataSource.setUsername(config.getUsername());
            hikariDataSource.setPassword(config.getPassword());
            hikariDataSource.setDriverClassName(config.getDriverClassName());

            // 设置连接池参数 - 使用用户配置的值
            hikariDataSource.setMaximumPoolSize(config.getMaximumPoolSize());
            hikariDataSource.setMinimumIdle(config.getMinimumIdle());
            hikariDataSource.setConnectionTimeout(config.getConnectionTimeout());
            // 使用默认的最大生命周期
            hikariDataSource.setMaxLifetime(dataSourceProperties.getMaxLifetime());
            hikariDataSource.setPoolName("ds-" + config.getId());

            // 不在创建时自动测试连接，延迟到用户主动测试时进行
            log.info("数据源[{}]配置已创建，连接将在使用时或手动测试时建立", config.getName());

            // 先保存配置到内存缓存
            dataSourceConfigCache.put(config.getId(), config);
            log.info("数据源[{}]配置已保存到缓存", config.getName());

            // 将数据源放入映射中，以便getDataSourceById能够找到它
            dataSourceMap.put(config.getId(), hikariDataSource);
            log.info("数据源[{}]已添加到运行时映射", config.getName());

            // 持久化配置到文件
            try {
                dataSourceRepository.save(config);
                log.info("数据源[{}]配置已持久化到文件", config.getName());
            } catch (Exception e) {
                log.error("数据源[{}]配置持久化失败: {}", config.getName(), e.getMessage(), e);
                // 不抛出异常，允许数据源在内存中创建成功
            }

            // 注意：这里不立即建立连接池，而是延迟到需要时
            // 连接池将在第一次调用 getConnection() 时才真正建立

        } catch (Exception e) {
            log.error("创建数据源配置失败 - 通用异常: {}", e.getMessage(), e);
            throw new RuntimeException("创建数据源配置失败: " + e.getMessage(), e);
        }
        log.info("数据源[{}]创建成功", config.getName());
        return config.getId();
    }
    
    /**
     * 根据ID获取数据源
     */
    public Optional<DataSource> getDataSourceById(String dataSourceId) {
        return Optional.ofNullable(dataSourceMap.get(dataSourceId));
    }

    /**
     * 根据ID获取运行时数据源配置（用于测试和状态检查）
     */
    public Optional<DataSourceConfig> getDataSourceConfigById(String dataSourceId) {
        // 优先从缓存中获取完整配置
        DataSourceConfig cachedConfig = dataSourceConfigCache.get(dataSourceId);
        if (cachedConfig != null) {
            return Optional.of(cachedConfig);
        }

        // 如果缓存中没有，检查数据源是否存在
        if (dataSourceMap.containsKey(dataSourceId)) {
            // 创建一个基本的配置对象用于状态检查
            DataSourceConfig config = new DataSourceConfig();
            config.setId(dataSourceId);
            config.setName("运行时数据源");
            config.setActive(true);
            config.setType("h2"); // 默认类型
            // 注意：这里不包含敏感信息如密码等，仅用于状态检查
            return Optional.of(config);
        }

        return Optional.empty();
    }

    /**
     * 获取所有运行时数据源配置
     */
    public Map<String, DataSourceConfig> getAllDataSourceConfigs() {
        Map<String, DataSourceConfig> allConfigs = new java.util.LinkedHashMap<>();

        // 先从缓存中获取完整配置
        allConfigs.putAll(dataSourceConfigCache);

        // 再检查运行时数据源，添加没有缓存的配置
        for (String dataSourceId : dataSourceMap.keySet()) {
            if (!allConfigs.containsKey(dataSourceId)) {
                DataSourceConfig config = new DataSourceConfig();
                config.setId(dataSourceId);
                config.setName("运行时数据源");
                config.setActive(true);
                config.setType("h2"); // 默认类型
                allConfigs.put(dataSourceId, config);
            }
        }

        return allConfigs;
    }
    
    /**
     * 设置默认数据源
     */
    public void setDefaultDataSource(DataSource defaultDataSource) {
        this.defaultDataSource = defaultDataSource;
    }
    
    /**
     * 设置当前活跃数据源
     */
    public void setActiveDataSource(String dataSourceId) {
        activeDataSourceId.set(dataSourceId);
    }
    
    /**
     * 获取当前数据源
     */
    public DataSource getCurrentDataSource() {
        String dataSourceId = DataSourceContextHolder.getDataSourceId();
        
        // 优先使用上下文中的数据源ID
        if (dataSourceId != null) {
            DataSource dataSource = dataSourceMap.get(dataSourceId);
            if (dataSource != null) {
                return dataSource;
            }
        }
        
        // 其次使用当前活跃的数据源ID
        String activeId = activeDataSourceId.get();
        if (activeId != null) {
            DataSource dataSource = dataSourceMap.get(activeId);
            if (dataSource != null) {
                return dataSource;
            }
        }
        
        // 只返回已配置的数据源，不再自动创建默认的H2内存数据库
        // 这样当没有用户配置的数据源时，不会显示任何数据库结构
        if (defaultDataSource == null && dataSourceMap.isEmpty() && activeDataSourceId.get() == null) {
            log.warn("没有配置任何数据源，返回null");
            return null;
        }
        
        return defaultDataSource;
    }
    
    /**
     * 测试连接
     */
    public boolean testConnection(DataSourceConfig config) throws SQLException, ClassNotFoundException {
        Class.forName(config.getDriverClassName());
        try (Connection conn = java.sql.DriverManager.getConnection(
                config.buildJdbcUrl(),
                config.getUsername(),
                config.getPassword())) {
            return conn.isValid(5); // 5秒超时
        }
    }
    
    /**
     * 更新数据源
     */
    public void updateDataSource(DataSourceConfig config) {
        String id = config.getId();
        
        // 先销毁旧数据源
        destroyDataSource(id);
        
        // 如果是激活状态，创建新数据源
        if (config.isActive()) {
            createDataSource(config);
        } else {
            // 非激活状态，只保存配置
            dataSourceRepository.save(config);
        }
    }
    
    /**
     * 销毁数据源
     */
    public void destroyDataSource(String id) {
        DataSource dataSource = dataSourceMap.remove(id);

        // 从缓存中移除配置
        dataSourceConfigCache.remove(id);
        log.info("数据源[{}]配置已从缓存中移除", id);

        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
            com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) dataSource;
            if (!hikariDataSource.isClosed()) {
                try {
                    hikariDataSource.close();
                } catch (Exception e) {
                    log.error("关闭数据源失败: {}", id, e);
                }
            }
        }

        // 从活跃数据源中移除
        if (activeDataSourceId.get() != null && activeDataSourceId.get().equals(id)) {
            activeDataSourceId.set(null);
        }
    }
    
    /**
     * 获取数据源池统计信息
     */
    public Map<String, Map<String, Object>> getDataSourcePoolStats() {
        Map<String, Map<String, Object>> stats = new ConcurrentHashMap<>();
        
        for (Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            DataSource ds = entry.getValue();
            if (ds instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) ds;
                Map<String, Object> dsStats = new ConcurrentHashMap<>();
                dsStats.put("activeConnections", hikariDataSource.getHikariPoolMXBean().getActiveConnections());
                dsStats.put("idleConnections", hikariDataSource.getHikariPoolMXBean().getIdleConnections());
                dsStats.put("maxPoolSize", hikariDataSource.getMaximumPoolSize());
                dsStats.put("minIdle", hikariDataSource.getMinimumIdle());
                stats.put(entry.getKey(), dsStats);
            }
        }
        
        return stats;
    }
    
    // DataSource接口实现
    @Override
    public Connection getConnection() throws SQLException {
        return getCurrentDataSource().getConnection();
    }
    
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getCurrentDataSource().getConnection(username, password);
    }
    
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getCurrentDataSource().unwrap(iface);
    }
    
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getCurrentDataSource().isWrapperFor(iface);
    }
    
    @Override
    public int getLoginTimeout() throws SQLException {
        return getCurrentDataSource().getLoginTimeout();
    }
    
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        getCurrentDataSource().setLoginTimeout(seconds);
    }
    
    @Override
    public java.io.PrintWriter getLogWriter() throws SQLException {
        return getCurrentDataSource().getLogWriter();
    }
    
    @Override
    public void setLogWriter(java.io.PrintWriter out) throws SQLException {
        getCurrentDataSource().setLogWriter(out);
    }
    
    @Override
    public java.util.logging.Logger getParentLogger() {
        try {
            return getCurrentDataSource().getParentLogger();
        } catch (Exception e) {
            log.warn("获取父Logger失败", e);
            return java.util.logging.Logger.getLogger("com.kami.springai.datasource");
        }
    }
}