package com.kami.springai.text2sql.service;

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

    private final DataSource dataSource;

    @Value("${spring.datasource.url:}")
    private String databaseUrl;

    /**
     * 测试数据库连接
     */
    public boolean testConnection() {
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
        try (Connection connection = dataSource.getConnection()) {
            return connection.getCatalog();
        } catch (Exception e) {
            log.error("获取数据库名称失败: {}", e.getMessage());
            return "unknown_database";
        }
    }

    /**
     * 发现数据库结构
     */
    public DatabaseSchema discoverSchema() {
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

        } catch (Exception e) {
            log.error("数据库结构发现失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据库结构发现失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定表的结构
     */
    public DatabaseSchema.TableSchema getTableSchema(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 获取表注释
            String tableComment = "";
            try (ResultSet tablesResult = metaData.getTables(databaseName, null, tableName, new String[]{"TABLE"})) {
                if (tablesResult.next()) {
                    tableComment = tablesResult.getString("REMARKS");
                }
            }
            
            List<DatabaseSchema.Column> columns = getTableColumns(metaData, databaseName, tableName);
            
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