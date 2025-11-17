package com.kami.springai.datasource.service;

import java.lang.reflect.Method;

/**
 * 数据源上下文持有类，用于存储当前线程的数据源ID
 * 支持ScopedValue（Java 21+）和ThreadLocal降级方案
 */
public class DataSourceContextHolder {
    // 尝试使用ScopedValue（Java 21+）
    private static final boolean SUPPORTS_SCOPED_VALUE;
    private static Object scopedValueInstance;
    private static Method scopedValueGetMethod;
    private static Method scopedValueRunWhereMethod;
    
    // 降级使用ThreadLocal
    private static final ThreadLocal<String> THREAD_LOCAL_DATA_SOURCE_ID = new ThreadLocal<>();
    
    static {
        boolean supports = false;
        try {
            // 尝试反射加载ScopedValue类
            Class<?> scopedValueClass = Class.forName("java.lang.ScopedValue");
            Method newInstanceMethod = scopedValueClass.getMethod("newInstance");
            scopedValueInstance = newInstanceMethod.invoke(null);
            scopedValueGetMethod = scopedValueClass.getMethod("get");
            scopedValueRunWhereMethod = scopedValueClass.getMethod("runWhere", scopedValueClass, Object.class, Runnable.class);
            supports = true;
        } catch (Exception e) {
            // ScopedValue不支持，将使用ThreadLocal
        }
        SUPPORTS_SCOPED_VALUE = supports;
    }
    
    /**
     * 设置数据源ID
     */
    public static void setDataSourceId(String dataSourceId) {
        if (SUPPORTS_SCOPED_VALUE) {
            // 使用ScopedValue（Java 21+）
            try {
                // 创建Runnable对象
                Runnable runnable = () -> {};
                // 作为数组传递参数
                Object[] args = {scopedValueInstance, dataSourceId, runnable};
                scopedValueRunWhereMethod.invoke(null, args);
                // 对于当前线程，直接设置ThreadLocal作为补充
                THREAD_LOCAL_DATA_SOURCE_ID.set(dataSourceId);
            } catch (Exception e) {
                // 降级到ThreadLocal
                THREAD_LOCAL_DATA_SOURCE_ID.set(dataSourceId);
            }
        } else {
            // 使用ThreadLocal
            THREAD_LOCAL_DATA_SOURCE_ID.set(dataSourceId);
        }
    }
    
    /**
     * 获取数据源ID
     */
    public static String getDataSourceId() {
        if (SUPPORTS_SCOPED_VALUE) {
            // 优先尝试从ScopedValue获取
            try {
                Object value = scopedValueGetMethod.invoke(scopedValueInstance);
                if (value instanceof String) {
                    return (String) value;
                }
            } catch (Exception e) {
                // 降级到ThreadLocal
            }
        }
        // 从ThreadLocal获取
        return THREAD_LOCAL_DATA_SOURCE_ID.get();
    }
    
    /**
     * 清理数据源上下文
     */
    public static void clear() {
        THREAD_LOCAL_DATA_SOURCE_ID.remove();
        // ScopedValue不需要手动清理，它会自动清理
    }
    
    /**
     * 检查是否支持ScopedValue
     */
    public static boolean isScopedValueSupported() {
        return SUPPORTS_SCOPED_VALUE;
    }
}