package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 条件语义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionSemantic {
    private String field;
    private String operator;
    private String value;
    private String conditionType;
}