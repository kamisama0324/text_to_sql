package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实体语义模型 - 识别和映射自然语言中的实体到数据库对象
 * 
 * 该类负责将用户查询中提到的实体（如"用户"、"订单"、"产品"等）映射到具体的数据库表和字段。
 * 实体识别是Text2SQL转换的关键步骤，它帮助系统理解用户查询涉及的数据对象。
 * 
 * @author Text2SQL-MCP
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitySemantic {
    
    /**
     * 实体名称 - 从用户查询中识别出的实体
     * 
     * 这是用户在自然语言中提到的实体名称。
     * 例如：
     * - "用户" - 指代系统中的用户
     * - "订单" - 指代订单数据
     * - "商品" - 指代商品信息
     * - "销售额" - 指代销售金额字段
     */
    private String entityName;
    
    /**
     * 语义类型 - 实体的语义分类
     * 
     * 用于标识实体在查询中的角色和类型：
     * - "TABLE" - 表示该实体对应一个数据表
     * - "COLUMN" - 表示该实体对应一个字段
     * - "VALUE" - 表示该实体是一个具体的值
     * - "METRIC" - 表示该实体是一个度量指标（如总数、平均值）
     * - "DIMENSION" - 表示该实体是一个维度（如时间、地区）
     * - "FUNCTION" - 表示该实体涉及函数操作（如最大、最小）
     */
    private String semanticType;
    
    /**
     * 对应的数据库表名
     * 
     * 实体映射到的具体数据库表名。
     * 例如：实体"用户"可能映射到"users"表，
     * 实体"订单"可能映射到"orders"表。
     * 如果实体是字段类型，这里存储该字段所属的表名。
     */
    private String tableName;
    
    /**
     * 是否为主要实体
     * 
     * true表示这是查询的核心实体，SQL生成应该围绕这个实体展开。
     * 例如："查询用户的订单"中，"用户"可能是主要实体。
     * 主要实体通常决定了FROM子句中的主表。
     */
    private boolean primary;
    
    /**
     * 置信度分数（0.0-1.0）
     * 
     * 表示系统对实体识别和映射的置信程度：
     * - 1.0 - 完全确定（实体名与数据库对象完全匹配）
     * - 0.8-0.9 - 高度确定（通过同义词或规则匹配）
     * - 0.5-0.8 - 中等确定（通过相似度或上下文推断）
     * - <0.5 - 低确定度（需要进一步确认）
     * 
     * 置信度用于在多个可能的映射中选择最佳方案。
     */
    private double confidence;
}