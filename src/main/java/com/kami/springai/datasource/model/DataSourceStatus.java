package com.kami.springai.datasource.model;

/**
 * 数据源状态枚举
 */
public enum DataSourceStatus {
    /**
     * 激活状态
     */
    ACTIVE,
    
    /**
     * 连接中
     */
    CONNECTING,
    
    /**
     * 连接失败
     */
    FAILED,
    
    /**
     * 已禁用
     */
    INACTIVE,
    
    /**
     * 未知状态
     */
    UNKNOWN;
    
    /**
     * 获取状态的中文描述
     */
    public String getDescription() {
        switch (this) {
            case ACTIVE:
                return "连接正常";
            case CONNECTING:
                return "连接中";
            case FAILED:
                return "连接失败";
            case INACTIVE:
                return "已禁用";
            case UNKNOWN:
                return "未知状态";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 获取状态的图标
     */
    public String getIcon() {
        switch (this) {
            case ACTIVE:
                return "✅";
            case CONNECTING:
                return "⏳";
            case FAILED:
                return "❌";
            case INACTIVE:
                return "🔴";
            case UNKNOWN:
                return "❓";
            default:
                return "❓";
        }
    }
}