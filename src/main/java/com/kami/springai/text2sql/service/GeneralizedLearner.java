package com.kami.springai.text2sql.service;

import com.kami.springai.text2sql.model.GeneralizedSqlPattern;
import com.kami.springai.text2sql.model.QuerySemantic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛化学习器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralizedLearner {

    /**
     * 从成功案例学习模式
     */
    public GeneralizedSqlPattern learnFromSuccess(String userQuery, String sql, QuerySemantic semantic) {
        log.debug("从成功案例学习: {}", userQuery);
        
        try {
            // 提取模式特征
            List<String> intentSemantics = extractIntentSemantics(semantic);
            List<String> entityTypes = extractEntityTypes(semantic);
            
            // 创建泛化模式
            return GeneralizedSqlPattern.builder()
                    .patternId(generatePatternId(intentSemantics, entityTypes))
                    .patternName(generatePatternName(userQuery))
                    .intentSemantics(intentSemantics)
                    .entityTypes(entityTypes)
                    .generalConfidence(0.7) // 初始置信度
                    .successCount(1)
                    .totalCount(1)
                    .build();
                    
        } catch (Exception e) {
            log.error("学习失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private List<String> extractIntentSemantics(QuerySemantic semantic) {
        List<String> semantics = new ArrayList<>();
        
        if (semantic != null && semantic.getIntent() != null) {
            semantics.add(semantic.getIntent().getPrimaryIntent());
            if (semantic.getIntent().getIntentSemantics() != null) {
                semantics.addAll(semantic.getIntent().getIntentSemantics());
            }
        }
        
        return semantics.isEmpty() ? List.of("default") : semantics;
    }

    private List<String> extractEntityTypes(QuerySemantic semantic) {
        List<String> types = new ArrayList<>();
        
        if (semantic != null && semantic.getEntities() != null) {
            for (var entity : semantic.getEntities()) {
                types.add(entity.getSemanticType());
            }
        }
        
        return types.isEmpty() ? List.of("default") : types;
    }

    private String generatePatternId(List<String> intentSemantics, List<String> entityTypes) {
        return String.format("pattern_%s_%s_%d", 
                String.join("_", intentSemantics),
                String.join("_", entityTypes),
                System.currentTimeMillis() % 10000);
    }

    private String generatePatternName(String userQuery) {
        // 简化的模式名称生成
        String[] words = userQuery.split("\\s+");
        if (words.length > 0) {
            return "Pattern_" + words[0];
        }
        return "UnknownPattern";
    }
}