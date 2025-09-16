package com.kami.springai.text2sql.validator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 验证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatorResult {
    private boolean valid;
    private String validatorName;
    private List<String> errors;
    private List<String> warnings;
    private double confidence;
    private long executionTimeMs;
    private String details;

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    public boolean isCritical() {
        return hasErrors() && errors.stream().anyMatch(error -> 
            error.contains("CRITICAL") || error.contains("SECURITY") || error.contains("SYNTAX"));
    }
}