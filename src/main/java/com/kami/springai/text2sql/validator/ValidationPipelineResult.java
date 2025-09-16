package com.kami.springai.text2sql.validator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 验证流水线结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationPipelineResult {
    private List<ValidatorResult> validatorResults;
    private boolean overallValid;
    private String overallMessage;
    private long totalExecutionTimeMs;

    public boolean hasErrors() {
        return validatorResults.stream().anyMatch(ValidatorResult::hasErrors);
    }

    public boolean hasWarnings() {
        return validatorResults.stream().anyMatch(ValidatorResult::hasWarnings);
    }

    public boolean hasCriticalErrors() {
        return validatorResults.stream().anyMatch(ValidatorResult::isCritical);
    }

    public List<String> getAllErrorMessages() {
        return validatorResults.stream()
                .filter(ValidatorResult::hasErrors)
                .flatMap(result -> result.getErrors().stream())
                .collect(Collectors.toList());
    }

    public List<String> getAllWarningMessages() {
        return validatorResults.stream()
                .filter(ValidatorResult::hasWarnings)
                .flatMap(result -> result.getWarnings().stream())
                .collect(Collectors.toList());
    }
}