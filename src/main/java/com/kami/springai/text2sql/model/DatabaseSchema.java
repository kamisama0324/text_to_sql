package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据库结构模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseSchema {
    private String databaseName;
    private List<Table> tables;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Table {
        private String name;
        private String comment;
        private List<Column> columns;
        private List<ForeignKey> foreignKeys;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Column {
        private String name;
        private String type;
        private boolean nullable;
        private boolean primaryKey;
        private String comment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForeignKey {
        private String columnName;
        private String referencedTable;
        private String referencedColumn;
        
        public String getRelationshipDescription() {
            return String.format("%s -> %s.%s", columnName, referencedTable, referencedColumn);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableSchema {
        private String tableName;
        private String tableComment;
        private List<Column> columns;
    }
}