package com.kami.springai.text2sql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 意图语义模型 - 用于解析和理解用户查询的意图
 * 
 * 该类负责捕获用户自然语言查询的核心意图信息，帮助系统理解用户想要执行的数据库操作类型。
 * 在Text2SQL转换过程中，意图识别是第一步，它决定了后续SQL生成的方向和策略。
 * 
 * @author Text2SQL-MCP
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentSemantic {
    
    /**
     * 主要意图 - 用户查询的核心目的
     * 
     * 示例值：
     * - "查询" - 用户想要查询数据
     * - "统计" - 用户想要进行统计分析
     * - "排序" - 用户想要对结果进行排序
     * - "筛选" - 用户想要筛选特定数据
     * - "聚合" - 用户想要聚合数据（如求和、平均等）
     */
    private String primaryIntent;
    
    /**
     * 查询类型 - SQL查询的具体类型
     * 
     * 可能的值：
     * - "SIMPLE_SELECT" - 简单查询（默认值）
     * - "AGGREGATE" - 聚合查询（包含COUNT、SUM、AVG等）
     * - "JOIN" - 连接查询（涉及多表关联）
     * - "SUBQUERY" - 子查询
     * - "UNION" - 联合查询
     * - "GROUP_BY" - 分组查询
     * - "ORDER_BY" - 排序查询
     */
    private String queryType;
    
    /**
     * 相关表名列表 - 查询涉及的数据库表
     * 
     * 通过意图分析识别出的可能相关的数据表名称。
     * 例如：用户说"查询用户订单"，则可能包含 ["users", "orders"]
     */
    private List<String> relevantTables;
    
    /**
     * 意图语义列表 - 细分的语义标签
     * 
     * 更细粒度的意图标签，用于精确描述查询的各个方面。
     * 例如：["时间范围查询", "最新数据", "分组统计", "条件过滤"]
     * 这些标签帮助系统更准确地生成SQL语句。
     */
    private List<String> intentSemantics;
    
    /**
     * 是否为复杂查询
     * 
     * true表示查询涉及多表关联、子查询、复杂条件等情况，
     * 需要更高级的SQL生成策略。
     * false表示简单的单表查询。
     */
    private boolean complexQuery;
    
    /**
     * 获取查询类型，如果未设置则返回默认值
     * 
     * @return 查询类型，默认为 "SIMPLE_SELECT"
     */
    public String getQueryType() {
        if (queryType == null) return "SIMPLE_SELECT";
        return queryType;
    }
}