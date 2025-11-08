package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 条件语义模型 - 解析查询中的过滤和条件信息
 * 
 * 该类负责捕获和表示用户查询中的各种条件约束，这些条件将被转换为SQL的WHERE、HAVING等子句。
 * 条件语义分析是生成准确SQL查询的核心，它确保查询结果符合用户的筛选要求。
 * 
 * @author Text2SQL-MCP
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionSemantic {
    
    /**
     * 条件字段 - 条件作用的数据库字段
     * 
     * 指定条件应用于哪个字段，可以是：
     * - 完整的字段名：如 "users.age"、"orders.status"
     * - 简单字段名：如 "age"、"status"（系统会自动推断所属表）
     * - 计算字段：如 "COUNT(*)"、"SUM(amount)"
     * 
     * 例如：
     * - "age" - 年龄字段
     * - "create_time" - 创建时间字段
     * - "orders.total_amount" - 订单表的总金额字段
     */
    private String field;
    
    /**
     * 操作符 - 条件比较操作符
     * 
     * 定义字段与值之间的比较关系：
     * - "=" - 等于
     * - "!=" 或 "<>" - 不等于
     * - ">" - 大于
     * - ">=" - 大于等于
     * - "<" - 小于
     * - "<=" - 小于等于
     * - "LIKE" - 模糊匹配
     * - "NOT LIKE" - 不匹配
     * - "IN" - 在列表中
     * - "NOT IN" - 不在列表中
     * - "BETWEEN" - 在范围内
     * - "IS NULL" - 为空
     * - "IS NOT NULL" - 不为空
     * - "EXISTS" - 存在（用于子查询）
     */
    private String operator;
    
    /**
     * 条件值 - 条件比较的目标值
     * 
     * 与字段进行比较的具体值，类型取决于操作符：
     * - 单个值：如 "18"、"'active'"、"'2024-01-01'"
     * - 列表值：如 "('pending', 'processing')" （用于IN操作）
     * - 范围值：如 "'2024-01-01' AND '2024-12-31'" （用于BETWEEN）
     * - 模式串：如 "%张%" （用于LIKE操作）
     * - NULL：当操作符是 IS NULL 或 IS NOT NULL 时可为空
     * 
     * 注意：字符串值应包含引号，日期值应使用适当的格式。
     */
    private String value;
    
    /**
     * 条件类型 - 条件的逻辑分类
     * 
     * 用于标识条件的类别和用途：
     * - "WHERE" - 普通的WHERE条件（行级过滤）
     * - "HAVING" - 聚合后的条件（组级过滤）
     * - "JOIN" - 连接条件（ON子句）
     * - "TIME_RANGE" - 时间范围条件
     * - "STATUS_FILTER" - 状态过滤条件
     * - "NUMERIC_RANGE" - 数值范围条件
     * - "TEXT_SEARCH" - 文本搜索条件
     * - "SUBQUERY" - 子查询条件
     * 
     * 条件类型帮助系统正确地将条件放置在SQL语句的相应位置。
     */
    private String conditionType;
}