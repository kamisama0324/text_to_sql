package com.kami.springai.text2sql.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL验证服务 - 集中管理所有SQL验证逻辑
 */
@Slf4j
@Service
public class SqlValidationService {
    
    // 危险关键字列表
    private static final List<String> DANGEROUS_KEYWORDS = List.of(
        "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE",
        "GRANT", "REVOKE", "EXEC", "EXECUTE", "UNION", "INTO OUTFILE",
        "INTO DUMPFILE", "LOAD_FILE", "BENCHMARK", "SLEEP"
    );
    
    // SQL长度限制
    private static final int MAX_SQL_LENGTH = 3000;
    
    // 允许的语句类型
    private static final Pattern ALLOWED_STATEMENT_PATTERN = 
        Pattern.compile("^\\s*(SELECT|SHOW|DESCRIBE|EXPLAIN)\\s+", Pattern.CASE_INSENSITIVE);
    
    // 注释模式
    private static final Pattern COMMENT_PATTERN = 
        Pattern.compile("(--.*?$|/\\*.*?\\*/|#.*?$)", Pattern.MULTILINE | Pattern.DOTALL);
    
    /**
     * 验证SQL的安全性
     * @param sql 待验证的SQL语句
     * @return 验证结果
     */
    public ValidationResult validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return ValidationResult.error("SQL语句不能为空");
        }
        
        // 1. 长度检查
        if (sql.length() > MAX_SQL_LENGTH) {
            return ValidationResult.error("SQL语句过长，最大允许 " + MAX_SQL_LENGTH + " 个字符");
        }
        
        // 2. 移除注释
        String cleanedSql = removeComments(sql);
        
        // 3. 检查允许的语句类型
        if (!ALLOWED_STATEMENT_PATTERN.matcher(cleanedSql).find()) {
            return ValidationResult.error("只允许执行SELECT、SHOW、DESCRIBE或EXPLAIN语句");
        }
        
        // 4. 检查危险关键字
        String upperSql = cleanedSql.toUpperCase();
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                return ValidationResult.error("SQL包含不允许的关键字: " + keyword);
            }
        }
        
        // 5. 检查分号（防止多语句执行）
        if (cleanedSql.split(";").length > 2) {
            return ValidationResult.error("不允许执行多条SQL语句");
        }
        
        // 6. 检查特殊字符和潜在注入
        if (containsSuspiciousPatterns(cleanedSql)) {
            return ValidationResult.error("SQL包含可疑的注入模式");
        }
        
        log.debug("SQL验证通过: {}", sql);
        return ValidationResult.success();
    }
    
    /**
     * 移除SQL中的注释
     */
    private String removeComments(String sql) {
        return COMMENT_PATTERN.matcher(sql).replaceAll(" ").trim();
    }
    
    /**
     * 检查可疑的SQL注入模式
     */
    private boolean containsSuspiciousPatterns(String sql) {
        // 检查常见的注入模式
        String[] suspiciousPatterns = {
            ".*\\bOR\\s+1\\s*=\\s*1.*",           // OR 1=1
            ".*\\bOR\\s+'[^']*'\\s*=\\s*'[^']*'.*", // OR 'x'='x'
            ".*\\bAND\\s+1\\s*=\\s*0.*",          // AND 1=0
            ".*\\bUNION\\s+ALL\\s+SELECT.*",      // UNION ALL SELECT
            ".*\\bSELECT\\s+.*\\s+FROM\\s+information_schema.*", // information_schema访问
            ".*\\bSELECT\\s+.*\\s+FROM\\s+mysql\\..*", // mysql系统表访问
            ".*\\bLOAD_FILE\\s*\\(.*",            // LOAD_FILE函数
            ".*\\bINTO\\s+OUTFILE.*",             // INTO OUTFILE
            ".*\\bINTO\\s+DUMPFILE.*"             // INTO DUMPFILE
        };
        
        String upperSql = sql.toUpperCase();
        for (String pattern : suspiciousPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(upperSql).matches()) {
                log.warn("检测到可疑的SQL注入模式: {}", pattern);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}