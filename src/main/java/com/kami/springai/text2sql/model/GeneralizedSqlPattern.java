package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 泛化SQL模式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralizedSqlPattern {
    private String patternId;
    private String patternName;
    private SqlTemplate sqlTemplate;
    private List<String> intentSemantics;
    private List<String> entityTypes;
    private double generalConfidence;
    private int successCount;
    private int totalCount;
    
    public boolean isHighQualityPattern() {
        return generalConfidence > 0.8 && successCount > 5;
    }
    
    public void recordSuccess(String database) {
        successCount++;
        totalCount++;
        updateConfidence();
    }
    
    public void recordFailure(String database) {
        totalCount++;
        updateConfidence();
    }
    
    private void updateConfidence() {
        if (totalCount > 0) {
            generalConfidence = (double) successCount / totalCount;
        }
    }
}

/**
 * SQL模板
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SqlTemplate {
    private String selectTemplate;
    private String fromTemplate;
    private String joinTemplate;
    private String whereTemplate;
    private String orderTemplate;
    private String limitTemplate;
    private Map<String, PlaceholderRule> placeholderRules;
}

/**
 * 占位符规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PlaceholderRule {
    private List<String> patterns;
    private String fallback;
    private String ruleType;
}