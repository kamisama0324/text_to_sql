package com.kami.springai.datasource.context;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据源上下文持有器
 * 用于在线程上下文中存储和获取当前使用的数据源ID
 */
@Slf4j
public class DataSourceContextHolder {
    
    /**
     * 使用ThreadLocal存储当前线程的数据源ID
     */
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置数据源ID
     * @param dataSourceId 数据源ID
     */
    public static void setDataSourceId(String dataSourceId) {
        log.debug("线程[{}]设置数据源ID: {}", Thread.currentThread().getName(), dataSourceId);
        CONTEXT_HOLDER.set(dataSourceId);
    }
    
    /**
     * 获取当前线程的数据源ID
     * @return 数据源ID
     */
    public static String getDataSourceId() {
        return CONTEXT_HOLDER.get();
    }
    
    /**
     * 清除数据源上下文
     */
    public static void clearDataSourceId() {
        log.debug("线程[{}]清除数据源上下文", Thread.currentThread().getName());
        CONTEXT_HOLDER.remove();
    }
    
    /**
     * 检查是否设置了数据源ID
     * @return 是否设置了数据源ID
     */
    public static boolean hasDataSourceId() {
        return CONTEXT_HOLDER.get() != null;
    }
}