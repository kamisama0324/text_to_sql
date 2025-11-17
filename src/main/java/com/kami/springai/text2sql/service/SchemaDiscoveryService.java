package com.kami.springai.text2sql.service;

import com.kami.springai.datasource.service.DataSourceContextHolder;
import com.kami.springai.datasource.service.DynamicDataSourceManager;
import com.kami.springai.text2sql.model.DatabaseSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库结构发现服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaDiscoveryService {

    private final DynamicDataSourceManager dynamicDataSourceManager;

    @Value("${spring.datasource.url:}")
    private String databaseUrl;

    /**
     * 测试数据库连接
     */
    public boolean testConnection() {
        DataSource dataSource = getCurrentDataSource();
        if (dataSource == null) {
            log.warn("没有配置数据源，无法测试连接");
            return false;
        }
        
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (Exception e) {
            log.error("数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前数据库名称
     */
    public String getCurrentDatabaseName() {
        DataSource dataSource = getCurrentDataSource();
        if (dataSource == null) {
            log.warn("没有配置数据源，无法获取数据库名称");
            return "";
        }
        
        try (Connection connection = dataSource.getConnection()) {
            return connection.getCatalog();
        } catch (Exception e) {
            log.error("获取数据库名称失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 发现数据库结构（使用当前上下文的数据源）
     */
    public DatabaseSchema discoverSchema() {
        String dataSourceId = DataSourceContextHolder.getDataSourceId();
        return discoverSchema(dataSourceId);
    }
    
    /**
     * 发现指定数据源的数据库结构
     */
    public DatabaseSchema discoverSchema(String dataSourceId) {
        try {
            // 使用指定数据源或当前上下文数据源
            DataSource dataSource = dataSourceId != null ? 
                dynamicDataSourceManager.getDataSourceById(dataSourceId).orElse(dynamicDataSourceManager.getCurrentDataSource()) :
                dynamicDataSourceManager.getCurrentDataSource();
                
            if (dataSource == null) {
                log.warn("没有配置数据源，无法发现数据库结构");
                return DatabaseSchema.builder()
                        .databaseName("")
                        .tables(new ArrayList<>())
                        .build();
            }
                
            try (Connection connection = dataSource.getConnection()) {
                String databaseName = connection.getCatalog();
                List<DatabaseSchema.Table> tables = new ArrayList<>();

                // 获取所有表
                DatabaseMetaData metaData = connection.getMetaData();
                try (ResultSet tablesResult = metaData.getTables(databaseName, null, "%", new String[]{"TABLE"})) {
                    while (tablesResult.next()) {
                        String tableName = tablesResult.getString("TABLE_NAME");
                        String tableComment = tablesResult.getString("REMARKS");
                        
                        List<DatabaseSchema.Column> columns = getTableColumns(metaData, databaseName, tableName);
                        List<DatabaseSchema.ForeignKey> foreignKeys = getTableForeignKeys(metaData, databaseName, tableName);
                        
                        tables.add(DatabaseSchema.Table.builder()
                                .name(tableName)
                                .comment(tableComment)
                                .columns(columns)
                                .foreignKeys(foreignKeys)
                                .build());
                    }
                }

                return DatabaseSchema.builder()
                        .databaseName(databaseName)
                        .tables(tables)
                        .build();
            }
        } catch (Exception e) {
            log.error("数据库结构发现失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据库结构发现失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前数据源
     */
    private DataSource getCurrentDataSource() {
        return dynamicDataSourceManager.getCurrentDataSource();
    }
    
    /**
     * 获取指定表的结构
     */
    public DatabaseSchema.TableSchema getTableSchema(String tableName) {
        DataSource dataSource = getCurrentDataSource();
        if (dataSource == null) {
            log.warn("没有配置数据源，无法获取表结构");
            return DatabaseSchema.TableSchema.builder()
                    .tableName(tableName)
                    .tableComment("")
                    .columns(new ArrayList<>())
                    .build();
        }
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseName = getCurrentDatabaseName();
            
            List<DatabaseSchema.Column> columns = getTableColumns(metaData, databaseName, tableName);
            List<DatabaseSchema.ForeignKey> foreignKeys = getTableForeignKeys(metaData, databaseName, tableName);
            
            String tableComment = "";
            try (ResultSet tableResult = metaData.getTables(databaseName, null, tableName, new String[]{"TABLE"})) {
                if (tableResult.next()) {
                    tableComment = tableResult.getString("REMARKS") != null ? tableResult.getString("REMARKS") : "";
                }
            }
            
            return DatabaseSchema.TableSchema.builder()
                    .tableName(tableName)
                    .tableComment(tableComment)
                    .columns(columns)
                    .build();
        } catch (Exception e) {
            log.error("获取表结构失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取表结构失败: " + e.getMessage());
        }
    }

    private List<DatabaseSchema.Column> getTableColumns(DatabaseMetaData metaData, String databaseName, String tableName) throws SQLException {
        List<DatabaseSchema.Column> columns = new ArrayList<>();
        
        // 获取主键信息
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet pkResult = metaData.getPrimaryKeys(databaseName, null, tableName)) {
            while (pkResult.next()) {
                primaryKeys.add(pkResult.getString("COLUMN_NAME"));
            }
        }

        // 获取列信息
        try (ResultSet columnsResult = metaData.getColumns(databaseName, null, tableName, "%")) {
            while (columnsResult.next()) {
                String columnName = columnsResult.getString("COLUMN_NAME");
                String columnType = columnsResult.getString("TYPE_NAME");
                int nullable = columnsResult.getInt("NULLABLE");
                String columnComment = columnsResult.getString("REMARKS");

                columns.add(DatabaseSchema.Column.builder()
                        .name(columnName)
                        .type(columnType)
                        .nullable(nullable == DatabaseMetaData.columnNullable)
                        .primaryKey(primaryKeys.contains(columnName))
                        .comment(columnComment)
                        .build());
            }
        }

        return columns;
    }

    private List<DatabaseSchema.ForeignKey> getTableForeignKeys(DatabaseMetaData metaData, String databaseName, String tableName) throws SQLException {
        List<DatabaseSchema.ForeignKey> foreignKeys = new ArrayList<>();

        try (ResultSet fkResult = metaData.getImportedKeys(databaseName, null, tableName)) {
            while (fkResult.next()) {
                String columnName = fkResult.getString("FKCOLUMN_NAME");
                String referencedTable = fkResult.getString("PKTABLE_NAME");
                String referencedColumn = fkResult.getString("PKCOLUMN_NAME");

                foreignKeys.add(DatabaseSchema.ForeignKey.builder()
                        .columnName(columnName)
                        .referencedTable(referencedTable)
                        .referencedColumn(referencedColumn)
                        .build());
            }
        }

        return foreignKeys;
    }
}