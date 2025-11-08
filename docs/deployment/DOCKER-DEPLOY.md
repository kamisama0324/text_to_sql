# SpringAI-MCP Docker 部署指南

## 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- DeepSeek API Key（必需）
- 可用的MySQL数据库（使用现有数据库或Docker中的MySQL）

## 快速开始

### 1. 设置环境变量

```bash
# 必需：设置DeepSeek API Key
export DEEPSEEK_API_KEY=your_api_key_here

# 可选：配置数据库连接（默认使用Docker中的MySQL）
export MYSQL_HOST=your_mysql_host      # 默认: mysql (Docker容器)
export MYSQL_PORT=3306                 # 默认: 3306
export MYSQL_DATABASE=your_database    # 默认: test
export MYSQL_USERNAME=your_username    # 默认: springai
export MYSQL_PASSWORD=your_password    # 默认: springai123
```

### 2. 使用启动脚本（推荐）

```bash
# 赋予执行权限
chmod +x start-docker.sh

# 方式1：交互式菜单
./start-docker.sh

# 方式2：命令行模式
./start-docker.sh start    # 构建并启动
./start-docker.sh stop     # 停止服务
./start-docker.sh restart  # 重启服务
./start-docker.sh logs     # 查看日志
```

### 3. 使用 Docker Compose（手动）

```bash
# 构建镜像
docker-compose build

# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f springai-app

# 停止服务
docker-compose down
```

## 服务说明

### 包含的服务

1. **SpringAI应用** (端口: 8090)
   - Text2SQL转换服务
   - Web界面
   
2. **MySQL数据库** (端口: 3307)
   - 可选，可使用现有数据库
   
3. **Redis缓存** (端口: 6380)
   - 用于缓存查询结果和数据库结构
   
4. **Nginx代理** (端口: 80/443)
   - 可选的反向代理

### 访问地址

- 应用主页：http://localhost:8090
- API文档：http://localhost:8090/swagger-ui.html (如配置)
- 健康检查：http://localhost:8090/actuator/health

## 配置选项

### 使用现有数据库

如果要连接现有的MySQL数据库，修改docker-compose.yml或设置环境变量：

```bash
# 使用外部数据库
MYSQL_HOST=192.168.1.100 \
MYSQL_PORT=3306 \
MYSQL_DATABASE=mydb \
MYSQL_USERNAME=myuser \
MYSQL_PASSWORD=mypass \
DEEPSEEK_API_KEY=xxx \
./start-docker.sh start
```

### 使用.env文件

创建 `.env` 文件（基于 `.env.example`）：

```bash
cp .env.example .env
# 编辑 .env 文件，填入实际配置
vim .env
```

### JVM调优

修改环境变量或docker-compose.yml中的JAVA_OPTS：

```yaml
environment:
  JAVA_OPTS: "-Xmx2g -Xms1g -XX:+UseZGC"
```

## 生产环境部署

### 1. 构建生产镜像

```bash
# 使用多阶段构建优化镜像大小
docker build -t springai-mcp:prod --build-arg PROFILE=prod .
```

### 2. 使用Docker Swarm或Kubernetes

```yaml
# docker-compose.prod.yml 示例
version: '3.8'
services:
  springai-app:
    image: springai-mcp:prod
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2'
          memory: 2G
```

### 3. 配置SSL证书

1. 将证书文件放在 `ssl/` 目录
2. 取消注释 `nginx.conf` 中的HTTPS配置
3. 重启Nginx容器

## 监控和维护

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs -f springai-app

# 查看最后100行日志
docker-compose logs --tail=100 springai-app
```

### 性能监控

```bash
# 查看容器资源使用情况
docker stats

# 查看容器详情
docker-compose ps
```

### 备份数据

```bash
# 备份MySQL数据
docker exec text2sql-mysql mysqldump -u root -p test > backup.sql

# 备份Redis数据
docker exec text2sql-redis redis-cli SAVE
docker cp text2sql-redis:/data/dump.rdb ./redis-backup.rdb
```

## 故障排除

### 1. 端口冲突

如果端口被占用，修改docker-compose.yml中的端口映射：

```yaml
ports:
  - "8091:8090"  # 改为其他端口
```

### 2. 内存不足

增加Docker内存限制或减小JVM堆内存：

```bash
JAVA_OPTS="-Xmx512m -Xms256m"
```

### 3. 数据库连接失败

检查数据库配置和网络连接：

```bash
# 测试数据库连接
docker exec springai-mcp curl telnet://mysql:3306
```

### 4. API Key无效

确认DeepSeek API Key正确且有效：

```bash
# 检查环境变量
docker exec springai-mcp env | grep DEEPSEEK
```

## 清理和卸载

```bash
# 停止并删除容器
docker-compose down

# 删除所有数据（包括数据卷）
docker-compose down -v

# 删除镜像
docker rmi springai-mcp:latest

# 使用脚本清理
./start-docker.sh clean
```

## 安全建议

1. **生产环境中修改默认密码**
2. **使用HTTPS和SSL证书**
3. **限制数据库访问权限**
4. **定期更新依赖和基础镜像**
5. **配置防火墙规则**
6. **不要在版本控制中提交敏感信息**

## 支持

如有问题，请查看：
- 项目文档：[README.md](./README.md)
- 日志文件：`./logs/` 目录
- GitHub Issues：[项目地址]