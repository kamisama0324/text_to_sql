package com.kami.springai.text2sql.service;

import com.kami.springai.text2sql.model.GeneralizedSqlPattern;
import com.kami.springai.text2sql.model.QuerySemantic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 双模式管理器 - 管理基础模式和用户增强模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DualPatternManager {

    private final GeneralizedLearner generalizedLearner;
    private final ConcurrentMap<String, GeneralizedSqlPattern> userPatterns = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, GeneralizedSqlPattern> basePatterns = new ConcurrentHashMap<>();

    /**
     * 查找匹配的模式
     */
    public List<GeneralizedSqlPattern> findMatchingPatterns(List<String> intentSemantics, List<String> entityTypes) {
        List<GeneralizedSqlPattern> matches = new ArrayList<>();
        
        // 先查找用户增强模式
        matches.addAll(findPatternsInMap(userPatterns, intentSemantics, entityTypes));
        
        // 再查找基础模式
        matches.addAll(findPatternsInMap(basePatterns, intentSemantics, entityTypes));
        
        // 按置信度排序
        return matches.stream()
                .sorted((p1, p2) -> Double.compare(p2.getGeneralConfidence(), p1.getGeneralConfidence()))
                .collect(Collectors.toList());
    }

    /**
     * 从用户反馈学习
     */
    public void learnFromUserFeedback(YamlConfigManager.FeedbackType feedbackType, 
                                     String userQuery, String generatedSql, 
                                     String correctedSql, QuerySemantic semantic) {
        log.info("处理用户反馈学习: {}", feedbackType);
        
        try {
            switch (feedbackType) {
                case POSITIVE:
                    handlePositiveFeedback(userQuery, generatedSql, semantic);
                    break;
                case NEGATIVE:
                    handleNegativeFeedback(userQuery, generatedSql, semantic);
                    break;
                case CORRECTION:
                    handleCorrectionFeedback(userQuery, generatedSql, correctedSql, semantic);
                    break;
            }
        } catch (Exception e) {
            log.error("用户反馈学习失败: {}", e.getMessage(), e);
        }
    }

    private List<GeneralizedSqlPattern> findPatternsInMap(ConcurrentMap<String, GeneralizedSqlPattern> patternMap,
                                                         List<String> intentSemantics, 
                                                         List<String> entityTypes) {
        return patternMap.values().stream()
                .filter(pattern -> hasSemanticMatch(pattern, intentSemantics, entityTypes))
                .collect(Collectors.toList());
    }

    private boolean hasSemanticMatch(GeneralizedSqlPattern pattern, 
                                   List<String> intentSemantics, 
                                   List<String> entityTypes) {
        // 检查意图语义匹配
        boolean intentMatch = pattern.getIntentSemantics().stream()
                .anyMatch(intentSemantics::contains);
        
        // 检查实体类型匹配
        boolean entityMatch = pattern.getEntityTypes().stream()
                .anyMatch(entityTypes::contains);
        
        return intentMatch || entityMatch;
    }

    private void handlePositiveFeedback(String userQuery, String generatedSql, QuerySemantic semantic) {
        GeneralizedSqlPattern pattern = generalizedLearner.learnFromSuccess(userQuery, generatedSql, semantic);
        if (pattern != null) {
            userPatterns.put(pattern.getPatternId(), pattern);
            log.info("添加正向反馈模式: {}", pattern.getPatternId());
        }
    }

    private void handleNegativeFeedback(String userQuery, String generatedSql, QuerySemantic semantic) {
        // 查找相似模式并降低其置信度
        List<String> intentSemantics = semantic.getIntent() != null ? 
                semantic.getIntent().getIntentSemantics() : List.of();
        List<String> entityTypes = semantic.getEntities() != null ?
                semantic.getEntities().stream().map(e -> e.getSemanticType()).collect(Collectors.toList()) : 
                List.of();
        
        List<GeneralizedSqlPattern> matches = findMatchingPatterns(intentSemantics, entityTypes);
        for (GeneralizedSqlPattern pattern : matches) {
            pattern.recordFailure("mysql");
            log.info("记录模式失败: {}", pattern.getPatternId());
        }
    }

    private void handleCorrectionFeedback(String userQuery, String generatedSql, 
                                        String correctedSql, QuerySemantic semantic) {
        // 先处理负向反馈
        handleNegativeFeedback(userQuery, generatedSql, semantic);
        
        // 再从修正的SQL学习新模式
        GeneralizedSqlPattern correctedPattern = generalizedLearner.learnFromSuccess(userQuery, correctedSql, semantic);
        if (correctedPattern != null) {
            userPatterns.put(correctedPattern.getPatternId(), correctedPattern);
            log.info("添加修正反馈模式: {}", correctedPattern.getPatternId());
        }
    }

    /**
     * 获取模式统计信息
     */
    public String getPatternStats() {
        return String.format("模式统计 - 用户模式: %d, 基础模式: %d", 
                userPatterns.size(), basePatterns.size());
    }
}

/**
 * 简化的配置管理器枚举
 */
class YamlConfigManager {
    public enum FeedbackType {
        POSITIVE, NEGATIVE, CORRECTION
    }
}