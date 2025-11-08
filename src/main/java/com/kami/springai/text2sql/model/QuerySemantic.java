package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询语义模型 - 自然语言查询的完整语义表示
 * 
 * 该类是Text2SQL转换的核心数据结构，它将用户的自然语言查询分解为结构化的语义组件。
 * QuerySemantic聚合了意图、实体、条件等所有语义信息，为SQL生成提供完整的语义基础。
 * 这是语义解析阶段的最终输出，也是SQL生成阶段的主要输入。
 * 
 * @author Text2SQL-MCP
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuerySemantic {
    
    /**
     * 查询意图 - 用户查询的核心意图和类型
     * 
     * 包含了查询的主要目的、查询类型、涉及的表等意图信息。
     * 这是理解用户想要什么的关键，决定了SQL的基本结构。
     * 
     * 例如：
     * - 用户说"统计每月销售额"，意图是"聚合统计"，类型是"GROUP_BY"
     * - 用户说"查找最新的10个订单"，意图是"查询"，类型是"ORDER_BY"
     * 
     * @see IntentSemantic
     */
    private IntentSemantic intent;
    
    /**
     * 实体列表 - 查询涉及的所有数据实体
     * 
     * 从用户查询中识别出的所有实体及其映射关系。
     * 这些实体将被转换为SQL中的表名、字段名或值。
     * 
     * 例如，查询"显示所有VIP用户的订单总额"可能包含：
     * - EntitySemantic{entityName="用户", tableName="users", semanticType="TABLE"}
     * - EntitySemantic{entityName="VIP", tableName="users.level", semanticType="VALUE"}
     * - EntitySemantic{entityName="订单", tableName="orders", semanticType="TABLE"}
     * - EntitySemantic{entityName="总额", tableName="orders.amount", semanticType="COLUMN"}
     * 
     * @see EntitySemantic
     */
    private List<EntitySemantic> entities;
    
    /**
     * 条件列表 - 查询的所有过滤和约束条件
     * 
     * 包含WHERE、HAVING、JOIN等各种条件的语义表示。
     * 这些条件确保查询结果符合用户的特定要求。
     * 
     * 例如，查询"查找今年创建的金额大于1000的订单"可能包含：
     * - ConditionSemantic{field="orders.create_time", operator=">=", value="2024-01-01", type="TIME_RANGE"}
     * - ConditionSemantic{field="orders.amount", operator=">", value="1000", type="NUMERIC_RANGE"}
     * 
     * 条件之间默认使用AND连接，复杂的OR条件需要特殊处理。
     * 
     * @see ConditionSemantic
     */
    private List<ConditionSemantic> conditions;
    
    /**
     * 整体置信度分数（0.0-1.0）
     * 
     * 表示系统对整个语义解析结果的置信程度。
     * 这是基于各个组件（意图、实体、条件）置信度的综合评分。
     * 
     * - 0.9-1.0：高置信度 - 语义明确，可以直接生成SQL
     * - 0.7-0.9：中等置信度 - 大部分语义清晰，可能需要默认值
     * - 0.5-0.7：低置信度 - 存在歧义，可能需要用户确认
     * - <0.5：极低置信度 - 需要重新解析或请求更多信息
     * 
     * 置信度影响SQL生成策略的选择和结果的可靠性评估。
     */
    private double confidence;
}