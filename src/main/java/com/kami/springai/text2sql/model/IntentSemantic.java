package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 意图语义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentSemantic {
    private String primaryIntent;
    private String queryType;
    private List<String> relevantTables;
    private List<String> intentSemantics;
    private boolean complexQuery;
    
    public String getQueryType() {
        if (queryType == null) return "SIMPLE_SELECT";
        return queryType;
    }
}