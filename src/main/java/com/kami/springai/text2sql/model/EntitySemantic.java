package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实体语义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitySemantic {
    private String entityName;
    private String semanticType;
    private String tableName;
    private boolean primary;
    private double confidence;
}