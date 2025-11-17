package com.kami.springai.datasource.controller;

import com.kami.springai.datasource.model.DataSourceConfig;
import com.kami.springai.datasource.model.DataSourceDTO;
import com.kami.springai.datasource.model.DataSourceStatus;
import com.kami.springai.datasource.service.DynamicDataSourceManager;
import com.kami.springai.datasource.service.DataSourceRepository;
import com.kami.springai.text2sql.service.SchemaDiscoveryService;
import com.kami.springai.text2sql.model.DatabaseSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 数据源管理REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/datasources")
public class DataSourceController {
    
    @Autowired
    private DynamicDataSourceManager dataSourceManager;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private SchemaDiscoveryService schemaDiscoveryService;
    
    /**
     * 获取所有数据源
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDataSources() {
        try {
            // 获取运行时缓存中的数据源配置
            Map<String, DataSourceConfig> runtimeConfigs = dataSourceManager.getAllDataSourceConfigs();

            // 获取持久化存储中的数据源
            List<DataSourceConfig> persistentConfigs = dataSourceRepository.findAll();

            // 合并两个数据源列表，以运行时为主
            Map<String, DataSourceConfig> allConfigs = new java.util.LinkedHashMap<>(runtimeConfigs);

            // 再添加持久化配置（不在运行时中的）
            for (DataSourceConfig config : persistentConfigs) {
                if (!allConfigs.containsKey(config.getId())) {
                    allConfigs.put(config.getId(), config);
                }
            }

            List<DataSourceDTO> dtos = allConfigs.values().stream()
                .map(config -> {
                    // 检查数据源状态
                    DataSourceStatus status = determineStatus(config);
                    return DataSourceDTO.fromConfig(config, status);
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dtos
            ));
        } catch (Exception e) {
            log.error("获取数据源列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "message", "获取数据源列表失败: " + e.getMessage()
                ));
        }
    }
    
    /**
     * 获取单个数据源
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDataSource(@PathVariable String id) {
        Optional<DataSourceConfig> configOpt = dataSourceRepository.findById(id);
        if (configOpt.isPresent()) {
            DataSourceConfig config = configOpt.get();
            DataSourceStatus status = determineStatus(config);
            DataSourceDTO dto = DataSourceDTO.fromConfig(config, status);
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", "数据源不存在"));
        }
    }
    
    /**
     * 创建数据源
     */
    @PostMapping
    public ResponseEntity<?> createDataSource(@RequestBody DataSourceConfig config) {
        try {
            // 验证必填字段
            validateConfig(config);

            // 创建数据源（不再自动测试连接和加载表结构）
            String id = dataSourceManager.createDataSource(config);
            log.info("数据源[{}]配置创建成功，可以手动测试连接和加载表结构", config.getName());

            Map<String, Object> responseData = Map.of(
                "id", id,
                "name", config.getName(),
                "status", "PENDING_CONNECTION"
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "数据源创建成功，请手动测试连接",
                "data", responseData
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "数据源创建失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新数据源
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDataSource(@PathVariable String id, @RequestBody DataSourceConfig config) {
        try {
            // 验证ID一致性
            if (!id.equals(config.getId())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "ID不一致"));
            }

            // 验证必填字段
            validateConfig(config);

            // 更新数据源
            dataSourceManager.updateDataSource(config);

            // 如果数据源是激活状态，尝试重新加载表结构
            DatabaseSchema schema = null;
            if (config.isActive()) {
                try {
                    schema = schemaDiscoveryService.discoverSchema(id);
                    log.info("数据源[{}]表结构重新加载成功，发现{}个表", config.getName(),
                            schema != null && schema.getTables() != null ? schema.getTables().size() : 0);
                } catch (Exception e) {
                    log.warn("数据源[{}]表结构重新加载失败: {}", config.getName(), e.getMessage());
                    // 不影响数据源更新成功，只记录警告
                }
            }

            Map<String, Object> responseData = Map.of("id", id, "name", config.getName());

            if (schema != null) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "数据源更新成功，表结构已重新加载",
                    "data", responseData,
                    "schema", schema
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "数据源更新成功",
                    "data", responseData
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "数据源更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除数据源
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDataSource(@PathVariable String id) {
        try {
            dataSourceManager.destroyDataSource(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "数据源删除成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "数据源删除失败: " + e.getMessage()));
        }
    }
    
    /**
     * 测试连接
     */
    @PostMapping("/test")
    public ResponseEntity<?> testConnection(@RequestBody DataSourceConfig config) {
        try {
            // 验证必填字段
            validateConfig(config);
            
            boolean success = dataSourceManager.testConnection(config);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "连接测试成功"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "连接测试失败，请检查配置"
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "连接测试失败: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 激活/停用数据源
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateDataSource(
            @PathVariable String id,
            @RequestParam(defaultValue = "true") boolean active) {
        try {
            Optional<DataSourceConfig> configOpt = dataSourceRepository.findById(id);
            if (configOpt.isPresent()) {
                DataSourceConfig config = configOpt.get();
                config.setActive(active);
                dataSourceManager.updateDataSource(config);

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", active ? "数据源已激活" : "数据源已停用"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "数据源不存在"));
            }
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "操作失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检查数据源连接状态
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<?> checkDataSourceStatus(@PathVariable String id) {
        try {
            // 优先从DynamicDataSourceManager的缓存中获取配置
            Optional<DataSourceConfig> configOpt = dataSourceManager.getDataSourceConfigById(id);
            if (!configOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "connected", false,
                    "message", "数据源不存在或未激活"
                ));
            }

            DataSourceConfig config = configOpt.get();

            // 检查数据源连接池中是否存在
            Optional<javax.sql.DataSource> dataSource = dataSourceManager.getDataSourceById(id);
            boolean isConnected = dataSource.isPresent();

            // 如果数据源存在，测试实际连接
            if (isConnected) {
                try {
                    boolean testResult = dataSourceManager.testConnection(config);
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "connected", testResult,
                        "status", "ACTIVE",
                        "message", testResult ? "数据源连接正常" : "数据源连接测试失败"
                    ));
                } catch (Exception e) {
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "connected", false,
                        "status", "ACTIVE",
                        "message", "连接测试异常: " + e.getMessage()
                    ));
                }
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "connected", false,
                    "status", config.isActive() ? "FAILED" : "INACTIVE",
                    "message", "数据源未激活或连接失败"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "connected", false,
                "message", "检查连接状态失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取数据源的表结构信息
     */
    @GetMapping("/{id}/schema")
    public ResponseEntity<?> getDataSourceSchema(@PathVariable String id) {
        try {
            DatabaseSchema schema = schemaDiscoveryService.discoverSchema(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", schema
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "获取表结构失败: " + e.getMessage()));
        }
    }

    /**
     * 获取测试数据源的表结构信息（用于测试前端解析）
     */
    @GetMapping("/test/schema")
    public ResponseEntity<?> getTestDataSourceSchema() {
        try {
            // 创建测试用的复杂数据库结构
            List<Map<String, Object>> tables = new ArrayList<>();

            // 表1: ability_market_character_type_workflows
            Map<String, Object> table1 = new HashMap<>();
            table1.put("name", "ability_market_character_type_workflows");
            table1.put("comment", "能力市场角色类型工作流表");

            List<Map<String, Object>> columns1 = new ArrayList<>();
            columns1.add(createColumn("id", "bigint", true, false, "主键ID"));
            columns1.add(createColumn("character_type_id", "bigint", false, false, "角色类型ID"));
            columns1.add(createColumn("workflow_id", "bigint", false, false, "工作流ID"));
            columns1.add(createColumn("step_order", "int", false, false, "步骤顺序"));
            columns1.add(createColumn("step_name", "varchar", false, true, "步骤名称"));
            columns1.add(createColumn("created_at", "timestamp", false, false, "创建时间"));
            columns1.add(createColumn("updated_at", "timestamp", false, false, "更新时间"));
            table1.put("columns", columns1);
            table1.put("foreignKeys", new ArrayList<>());
            tables.add(table1);

            // 表2: echoing_characters
            Map<String, Object> table2 = new HashMap<>();
            table2.put("name", "echoing_characters");
            table2.put("comment", "回声角色表");

            List<Map<String, Object>> columns2 = new ArrayList<>();
            columns2.add(createColumn("id", "bigint", true, false, "主键ID"));
            columns2.add(createColumn("character_name", "varchar", false, false, "角色名称"));
            columns2.add(createColumn("character_type", "varchar", false, true, "角色类型"));
            columns2.add(createColumn("description", "text", false, true, "角色描述"));
            columns2.add(createColumn("avatar_url", "varchar", false, true, "头像URL"));
            columns2.add(createColumn("voice_config", "json", false, true, "语音配置"));
            columns2.add(createColumn("created_by", "bigint", false, false, "创建者ID"));
            columns2.add(createColumn("created_at", "timestamp", false, false, "创建时间"));
            columns2.add(createColumn("updated_at", "timestamp", false, false, "更新时间"));
            table2.put("columns", columns2);
            table2.put("foreignKeys", new ArrayList<>());
            tables.add(table2);

            // 表3: cloud_device
            Map<String, Object> table3 = new HashMap<>();
            table3.put("name", "cloud_device");
            table3.put("comment", "云设备表");

            List<Map<String, Object>> columns3 = new ArrayList<>();
            columns3.add(createColumn("id", "bigint", true, false, "主键ID"));
            columns3.add(createColumn("device_id", "varchar", false, false, "设备唯一标识"));
            columns3.add(createColumn("device_name", "varchar", false, false, "设备名称"));
            columns3.add(createColumn("device_type", "varchar", false, false, "设备类型"));
            columns3.add(createColumn("status", "varchar", false, false, "设备状态"));
            columns3.add(createColumn("location", "varchar", false, true, "设备位置"));
            columns3.add(createColumn("ip_address", "varchar", false, true, "IP地址"));
            columns3.add(createColumn("last_heartbeat", "timestamp", false, true, "最后心跳时间"));
            columns3.add(createColumn("created_at", "timestamp", false, false, "创建时间"));
            columns3.add(createColumn("updated_at", "timestamp", false, false, "更新时间"));
            table3.put("columns", columns3);
            table3.put("foreignKeys", new ArrayList<>());
            tables.add(table3);

            Map<String, Object> schema = new HashMap<>();
            schema.put("databaseName", "test_complex_db");
            schema.put("tables", tables);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", schema
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "获取测试表结构失败: " + e.getMessage()));
        }
    }

    private Map<String, Object> createColumn(String name, String type, boolean primaryKey, boolean nullable, String comment) {
        Map<String, Object> column = new HashMap<>();
        column.put("name", name);
        column.put("type", type);
        column.put("primaryKey", primaryKey);
        column.put("nullable", nullable);
        column.put("comment", comment);
        return column;
    }

    /**
     * 获取数据源池统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getDataSourceStats() {
        Map<String, Map<String, Object>> stats = dataSourceManager.getDataSourcePoolStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 验证配置
     */
    private void validateConfig(DataSourceConfig config) {
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("数据源名称不能为空");
        }
        if (config.getType() == null || config.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("数据库类型不能为空");
        }
        if (config.getHost() == null || config.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("主机地址不能为空");
        }
        if (config.getDatabase() == null || config.getDatabase().trim().isEmpty()) {
            throw new IllegalArgumentException("数据库名称不能为空");
        }
        if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
          if (config.getPassword() == null ) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }
    
    /**
     * 确定数据源状态
     */
    private DataSourceStatus determineStatus(DataSourceConfig config) {
        if (!config.isActive()) {
            return DataSourceStatus.INACTIVE;
        }
        
        // 检查数据源是否在池中有活跃连接
        Optional<?> dataSource = dataSourceManager.getDataSourceById(config.getId());
        if (dataSource.isPresent()) {
            return DataSourceStatus.ACTIVE;
        } else {
            // 尝试测试连接
            try {
                boolean testSuccess = dataSourceManager.testConnection(config);
                return testSuccess ? DataSourceStatus.ACTIVE : DataSourceStatus.FAILED;
            } catch (SQLException e) {
                return DataSourceStatus.FAILED;
            } catch (ClassNotFoundException e) {
                return DataSourceStatus.FAILED;
            }
        }
    }
}