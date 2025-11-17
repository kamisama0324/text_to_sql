package com.kami.springai.datasource.test;

import com.kami.springai.datasource.model.DataSourceConfig;
import com.kami.springai.datasource.model.DataSourceDTO;
import com.kami.springai.datasource.service.DynamicDataSourceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源测试控制器
 * 用于验证动态数据源功能
 */
@Slf4j
@RestController
@RequestMapping("/api/test/datasource")
@RequiredArgsConstructor
public class DataSourceTestController {
    
    private final DynamicDataSourceManager dataSourceManager;
    
    /**
     * 测试数据源连接
     */
    @GetMapping("/connection")
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取当前数据源
            DataSource currentDataSource = dataSourceManager.getCurrentDataSource();
            
            // 测试连接
            try (Connection connection = currentDataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                String databaseName = connection.getCatalog();
                
                result.put("success", true);
                result.put("message", "连接成功");
                result.put("databaseName", databaseName);
                result.put("dataSourceClass", currentDataSource.getClass().getName());
            }
            
        } catch (Exception e) {
            log.error("数据源连接测试失败", e);
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取数据源状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取数据源池统计信息
            Map<String, Map<String, Object>> stats = dataSourceManager.getDataSourcePoolStats();
            
            result.put("success", true);
            result.put("message", "获取成功");
            result.put("poolStats", stats);
            result.put("dataSourceCount", stats.size());
            
        } catch (Exception e) {
            log.error("获取数据源状态失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }
        
        return result;
    }
}