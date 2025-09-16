# SpringAI-MCP 项目文档中心

> **SpringAI Model Context Protocol** 智能数据库查询平台的完整文档集合

---

## 📚 核心文档

### 🏗️ 项目架构文档
- **[项目架构总览](./architecture/project-overview.md)** - 系统架构、技术栈、模块划分详解
- **[核心流程详解](./architecture/process-flows.md)** - Text2SQL、缓存、MCP集成等核心流程

### 📚 API 接口文档  
- **[接口参考手册](./api/endpoints-reference.md)** - 完整的 RESTful API 接口文档

### 🚀 开发与优化
- **[性能优化路线图](./development/optimization-roadmap.md)** - 四阶段优化计划与实施建议
- **[优化建议总结](./development/optimization-summary.md)** - 基于项目分析的具体优化方案

### 🛠️ 部署运维
- **[生产环境部署指南](./deployment/production-guide.md)** - Docker、Kubernetes 部署配置

### 📋 历史文档
- **[快速启动指南](./guides/快速启动指南.md)** - 15分钟从0到可用的完整指南 
- **[项目进度报告20250908](./progress-reports/项目进度报告20250908.md)** - 历史项目进度
- **[Text2SQL-MCP设计方案](../Text2SQL-MCP设计方案.md)** - 原始设计方案

## 🎯 项目概况

SpringAI-MCP 是基于 Spring Boot 3.4.1 和 Java 24 的智能数据库查询平台，实现自然语言到 SQL 的转换。

### 核心特性
- 🧠 **智能 SQL 生成**: DeepSeek API 驱动的自然语言处理
- 🚀 **高性能架构**: Java 24 虚拟线程 + 结构化并发  
- 🔒 **企业级安全**: 多层 SQL 注入防护
- 🎯 **机器学习**: 基于反馈的模式学习能力

### 技术栈
| 组件 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.4.1 |
| AI集成 | Spring AI | 1.0.0-M5 |
| 数据库 | MySQL | 8.0+ |
| 前端 | Vue.js 3 | 3.x |
| 缓存 | Redis + 本地缓存 | - |

## 📈 性能指标

### 当前性能
- **首次查询响应**: ~14秒 (含数据库结构发现)
- **缓存命中查询**: ~13秒  
- **并发处理**: 基于虚拟线程，理论无限制
- **数据库支持**: MySQL (PostgreSQL 规划中)

### 主要瓶颈
1. AI API 调用延迟 (80-90% 响应时间)
2. 数据库结构首次发现耗时
3. 前端资源加载依赖外部 CDN

## 🔧 快速开始

### 启动应用
```bash
./gradlew bootRun
```

### 访问地址  
- **主界面**: http://localhost:8080
- **Text2SQL**: http://localhost:8080/text2sql
- **健康检查**: http://localhost:8080/api/health

### 测试查询示例
```
查询所有用户信息
统计每个部门的员工数量
查找最近一周注册的用户
```

## 🛣️ 优化路线图

### 阶段一: 性能优化 (1-2周) 🚀
- **AI 响应缓存**: 相似查询从 13s 降至 < 1s
- **数据库结构压缩**: 减少 AI 处理时间 30-40%
- **前端资源本地化**: 页面加载速度提升 60%

### 阶段二: 扩展性优化 (2-4周) 🏗️
- **Redis 分布式缓存**: 多级缓存架构
- **多数据库支持**: PostgreSQL 适配
- **API 限流监控**: 生产环境保护

### 阶段三: 企业级功能 (1-2月) 🏢  
- **用户认证授权**: OAuth2 集成
- **查询审计日志**: 完整操作追踪
- **多轮对话支持**: 上下文理解

### 阶段四: 平台化演进 (3-6月) 🌐
- **微服务架构**: 服务拆分独立部署
- **多租户支持**: SaaS 化改造
- **数据可视化**: 图表自动生成

---

## 🗂️ 文档分类

### 按模块分类

#### 🏗️ 架构文档 (`architecture/`)
系统架构设计和核心流程说明
- `project-overview.md` - 项目架构总览
- `process-flows.md` - 核心流程详解

#### 📚 API文档 (`api/`) 
接口规范和使用说明
- `endpoints-reference.md` - 完整API接口参考

#### 🚀 开发文档 (`development/`)
开发指南和优化方案
- `optimization-roadmap.md` - 性能优化路线图
- `optimization-summary.md` - 优化建议总结

#### 🛠️ 部署文档 (`deployment/`)
生产环境配置和运维指南
- `production-guide.md` - 生产环境部署指南

### 按受众分类

#### 👨‍💻 开发者文档
- [项目架构总览](./architecture/project-overview.md) - 了解系统设计
- [核心流程详解](./architecture/process-flows.md) - 深入技术实现
- [接口参考手册](./api/endpoints-reference.md) - API集成开发
- [性能优化路线图](./development/optimization-roadmap.md) - 技术优化方向

#### 👔 管理人员文档  
- [项目架构总览](./architecture/project-overview.md) - 整体技术方案
- [性能优化路线图](./development/optimization-roadmap.md) - 发展规划
- [生产环境部署指南](./deployment/production-guide.md) - 商用部署

#### 🔧 运维人员文档
- [生产环境部署指南](./deployment/production-guide.md) - 部署配置
- [接口参考手册](./api/endpoints-reference.md) - 监控接口

---

## 🎯 文档更新记录

| 日期 | 文档 | 更新内容 | 作者 |
|------|------|----------|------|
| 2025-09-11 | 项目架构总览 | 完整系统架构分析，技术栈详解，性能指标 | Claude |
| 2025-09-11 | 核心流程详解 | Text2SQL转换流程，缓存机制，MCP集成详解 | Claude |
| 2025-09-11 | 接口参考手册 | 完整API文档，安全限制，客户端集成示例 | Claude |
| 2025-09-11 | 性能优化路线图 | 四阶段优化计划，技术方案，实施建议 | Claude |
| 2025-09-11 | 生产环境部署指南 | Docker/K8s配置，监控方案，扩容策略 | Claude |
| 2025-09-11 | 文档中心 | 创建新的文档结构和导航系统 | Claude |

## 📋 项目状态

### ✅ 已完成
- 核心 Text2SQL 转换功能
- 安全的 SQL 执行机制
- 本地缓存系统优化
- Vue.js 现代化前端
- 完整的项目文档体系

### 🔄 进行中  
- 性能优化和缓存改进
- 代码架构重构完善
- 系统集成测试

### 📋 待开发
- Redis 分布式缓存集成
- 多数据库支持扩展
- API 限流和监控
- 用户认证授权系统

## 🔍 快速导航

**我想要...**

- **🚀 立即启动项目** → [快速启动指南](./guides/快速启动指南.md)
- **🏗️ 了解系统架构** → [项目架构总览](./architecture/project-overview.md)
- **📊 查看核心流程** → [核心流程详解](./architecture/process-flows.md)
- **📖 查看API文档** → [接口参考手册](./api/endpoints-reference.md)
- **🚀 了解优化方案** → [性能优化路线图](./development/optimization-roadmap.md)
- **🛠️ 生产部署** → [生产环境部署指南](./deployment/production-guide.md)

## 📞 支持与反馈

如有问题或建议，请通过以下方式联系：
- 项目仓库: 提交 Issue 
- 技术文档: 参考本文档目录
- 快速问题: 查看 [接口参考手册](./api/endpoints-reference.md)

---

*📅 最后更新: 2025-09-11*  
*🏷️ 版本: 2.0.0*  
*👨‍💻 维护者: SpringAI-MCP Team*