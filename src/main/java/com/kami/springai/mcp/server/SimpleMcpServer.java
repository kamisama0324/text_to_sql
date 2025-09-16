package com.kami.springai.mcp.server;

import com.kami.springai.text2sql.model.DatabaseSchema;
import com.kami.springai.text2sql.service.SchemaDiscoveryService;
import com.kami.springai.text2sql.service.SqlExecutionService;
import com.kami.springai.text2sql.service.Text2SqlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 集成了真实Text2SQL功能的MCP服务器实现
 * 
 * @author Text2SQL-MCP
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleMcpServer {

    private final SchemaDiscoveryService schemaDiscoveryService;
    private final Text2SqlService text2SqlService;
    private final SqlExecutionService sqlExecutionService;

    /**
     * 获取服务器信息
     */
    public ServerInfo getServerInfo() {
        return ServerInfo.builder()
                .name("Text2SQL MCP Server")
                .version("1.0.0")
                .description("提供自然语言转SQL查询功能的MCP服务器")
                .build();
    }

    /**
     * 获取可用工具列表
     */
    public ToolsList getAvailableTools() {
        return ToolsList.builder()
                .tools(java.util.List.of(
                    ToolInfo.builder()
                        .name("text_to_sql")
                        .description("将自然语言查询转换为SQL语句")
                        .parameters(java.util.Map.of(
                            "query", "自然语言查询内容",
                            "context", "额外上下文信息（可选）"
                        ))
                        .build(),
                    ToolInfo.builder()
                        .name("execute_sql")
                        .description("执行SQL查询并返回结果")
                        .parameters(java.util.Map.of(
                            "sql", "要执行的SQL语句"
                        ))
                        .build(),
                    ToolInfo.builder()
                        .name("get_database_schema")
                        .description("获取数据库结构信息")
                        .parameters(java.util.Map.of(
                            "table_name", "表名（可选）"
                        ))
                        .build(),
                    ToolInfo.builder()
                        .name("explain_sql")
                        .description("解释SQL查询语句的含义")
                        .parameters(java.util.Map.of(
                            "sql", "要解释的SQL语句"
                        ))
                        .build()
                ))
                .build();
    }

    /**
     * 执行工具调用
     */
    public ToolResult executeTool(String toolName, java.util.Map<String, Object> parameters) {
        log.info("执行工具调用: {} with parameters: {}", toolName, parameters);
        
        try {
            return switch (toolName) {
                case "text_to_sql" -> handleTextToSql(parameters);
                case "execute_sql" -> handleExecuteSql(parameters);
                case "get_database_schema" -> handleGetDatabaseSchema(parameters);
                case "explain_sql" -> handleExplainSql(parameters);
                default -> ToolResult.error("Unknown tool: " + toolName);
            };
        } catch (Exception e) {
            log.error("Tool execution failed: {}", e.getMessage(), e);
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    private ToolResult handleTextToSql(java.util.Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        String context = (String) parameters.get("context");
        
        if (query == null || query.trim().isEmpty()) {
            return ToolResult.error("Query parameter is required");
        }
        
        try {
            // 使用真实的Text2SQL服务
            String sql = text2SqlService.convertToSql(query, context);
            String explanation = text2SqlService.explainSql(sql, context);
            
            String result = String.format("""
                自然语言查询: %s
                
                生成的SQL:
                ```sql
                %s
                ```
                
                查询说明:
                %s
                """, query, sql, explanation);
            
            return ToolResult.success(result);
            
        } catch (Exception e) {
            log.error("Text2SQL转换失败: {}", e.getMessage(), e);
            return ToolResult.error("Text2SQL转换失败: " + e.getMessage());
        }
    }

    private ToolResult handleExecuteSql(java.util.Map<String, Object> parameters) {
        String sql = (String) parameters.get("sql");
        
        if (sql == null || sql.trim().isEmpty()) {
            return ToolResult.error("SQL parameter is required");
        }
        
        try {
            // 使用真实的SQL执行服务
            SqlExecutionService.QueryResult queryResult = sqlExecutionService.executeQuery(sql);
            
            if (!queryResult.isSuccess()) {
                return ToolResult.error("SQL执行失败: " + queryResult.getErrorMessage());
            }
            
            StringBuilder result = new StringBuilder();
            result.append("查询执行成功\n");
            result.append(String.format("返回行数: %d\n", queryResult.getTotalRows()));
            result.append(String.format("执行耗时: %d ms\n\n", queryResult.getExecutionTime()));
            
            if (queryResult.getTotalRows() > 0) {
                result.append("列名: ").append(String.join(", ", queryResult.getColumns())).append("\n\n");
                
                int maxRows = Math.min(10, queryResult.getRows().size());
                result.append("查询结果（前").append(maxRows).append("行）:\n");
                
                for (int i = 0; i < maxRows; i++) {
                    result.append(String.format("%d. %s\n", i + 1, queryResult.getRows().get(i)));
                }
                
                if (queryResult.getTotalRows() > maxRows) {
                    result.append(String.format("... 还有 %d 行数据\n", queryResult.getTotalRows() - maxRows));
                }
            }
            
            return ToolResult.success(result.toString());
            
        } catch (Exception e) {
            log.error("SQL执行失败: {}", e.getMessage(), e);
            return ToolResult.error("SQL执行失败: " + e.getMessage());
        }
    }

    private ToolResult handleGetDatabaseSchema(java.util.Map<String, Object> parameters) {
        String tableName = (String) parameters.get("table_name");
        
        try {
            if (tableName != null && !tableName.trim().isEmpty()) {
                // 获取指定表的结构
                DatabaseSchema.TableSchema tableSchema = schemaDiscoveryService.getTableSchema(tableName);
                String result = formatTableSchema(tableSchema);
                return ToolResult.success(result);
            } else {
                // 获取整个数据库结构
                DatabaseSchema schema = schemaDiscoveryService.discoverSchema();
                String result = formatDatabaseSchema(schema);
                return ToolResult.success(result);
            }
        } catch (Exception e) {
            log.error("获取数据库结构失败: {}", e.getMessage(), e);
            return ToolResult.error("获取数据库结构失败: " + e.getMessage());
        }
    }

    private ToolResult handleExplainSql(java.util.Map<String, Object> parameters) {
        String sql = (String) parameters.get("sql");
        String context = (String) parameters.get("context");
        
        if (sql == null || sql.trim().isEmpty()) {
            return ToolResult.error("SQL parameter is required");
        }
        
        try {
            // 使用真实的SQL解释服务
            String explanation = text2SqlService.explainSql(sql, context);
            return ToolResult.success(explanation);
            
        } catch (Exception e) {
            log.error("SQL解释失败: {}", e.getMessage(), e);
            return ToolResult.error("SQL解释失败: " + e.getMessage());
        }
    }

    private String formatDatabaseSchema(DatabaseSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("数据库结构信息:\n");
        sb.append("数据库名: ").append(schema.getDatabaseName()).append("\n\n");
        
        for (DatabaseSchema.Table table : schema.getTables()) {
            sb.append("表名: ").append(table.getName()).append("\n");
            if (table.getComment() != null && !table.getComment().trim().isEmpty()) {
                sb.append("说明: ").append(table.getComment()).append("\n");
            }
            sb.append("列信息:\n");
            for (DatabaseSchema.Column column : table.getColumns()) {
                sb.append(String.format("  - %s (%s)%s%s\n", 
                    column.getName(), 
                    column.getType(),
                    column.isPrimaryKey() ? " [主键]" : "",
                    column.getComment() != null && !column.getComment().trim().isEmpty() 
                        ? " - " + column.getComment() : ""
                ));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }

    private String formatTableSchema(DatabaseSchema.TableSchema table) {
        StringBuilder sb = new StringBuilder();
        sb.append("表结构信息:\n");
        sb.append("表名: ").append(table.getTableName()).append("\n");
        if (table.getTableComment() != null && !table.getTableComment().trim().isEmpty()) {
            sb.append("说明: ").append(table.getTableComment()).append("\n");
        }
        sb.append("列信息:\n");
        for (DatabaseSchema.Column column : table.getColumns()) {
            sb.append(String.format("  - %s (%s)%s%s\n", 
                column.getName(), 
                column.getType(),
                column.isPrimaryKey() ? " [主键]" : "",
                column.getComment() != null && !column.getComment().trim().isEmpty() 
                    ? " - " + column.getComment() : ""
            ));
        }
        
        return sb.toString();
    }

    // 内部数据类
    @lombok.Builder
    @lombok.Data
    public static class ServerInfo {
        private String name;
        private String version;
        private String description;
    }

    @lombok.Builder
    @lombok.Data
    public static class ToolsList {
        private java.util.List<ToolInfo> tools;
    }

    @lombok.Builder
    @lombok.Data
    public static class ToolInfo {
        private String name;
        private String description;
        private java.util.Map<String, String> parameters;
    }

    @lombok.Builder
    @lombok.Data
    public static class ToolResult {
        private boolean success;
        private String content;
        private String error;
        
        public static ToolResult success(String content) {
            return ToolResult.builder()
                    .success(true)
                    .content(content)
                    .build();
        }
        
        public static ToolResult error(String error) {
            return ToolResult.builder()
                    .success(false)
                    .error(error)
                    .build();
        }
    }
}