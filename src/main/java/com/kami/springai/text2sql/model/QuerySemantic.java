package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询语义模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuerySemantic {
    private IntentSemantic intent;
    private List<EntitySemantic> entities;
    private List<ConditionSemantic> conditions;
    private double confidence;
}