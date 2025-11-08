# 多阶段构建 - 第一阶段：构建应用
FROM azul/zulu-openjdk:24 AS builder

WORKDIR /app

# 复制gradle文件
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# 下载依赖（利用Docker缓存）
RUN ./gradlew dependencies --no-daemon

# 复制源代码
COPY src src

# 构建应用
RUN ./gradlew bootJar --no-daemon

# 多阶段构建 - 第二阶段：运行环境
FROM azul/zulu-openjdk:24-jre

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    nodejs \
    npm \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=builder /app/build/libs/*.jar app.jar

# 复制静态资源（如果有单独的前端资源）
COPY --from=builder /app/src/main/resources/static /app/static

# 创建日志目录
RUN mkdir -p /app/logs && chown -R spring:spring /app

# 切换到非root用户
USER spring

# 暴露端口
EXPOSE 8090

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# JVM参数优化 - 使用JDK 24的虚拟线程特性
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseZGC \
    -XX:MaxGCPauseMillis=200 \
    --enable-preview \
    -Dspring.profiles.active=docker \
    -Dspring.threads.virtual.enabled=true \
    -Djava.security.egd=file:/dev/./urandom"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]