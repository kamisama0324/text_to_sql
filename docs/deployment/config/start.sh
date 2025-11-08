#!/bin/bash

echo "=== Spring AI MCP 项目启动脚本 ==="

# 检查Java版本
echo "检查Java版本..."
java -version

# 检查Node.js是否安装
echo "检查Node.js..."
if ! command -v node &> /dev/null; then
    echo "警告: Node.js未安装，MCP服务器可能无法正常工作"
    echo "请安装Node.js: https://nodejs.org/"
else
    echo "Node.js版本: $(node --version)"
fi

# 检查npm是否安装
if ! command -v npm &> /dev/null; then
    echo "警告: npm未安装"
else
    echo "npm版本: $(npm --version)"
fi

# 安装MCP服务器包（如果需要）
echo "检查MCP服务器包..."
echo "如果需要安装MCP服务器包，请运行："
echo "npm install -g @modelcontextprotocol/server-filesystem"
echo "npm install -g @modelcontextprotocol/server-github"

# 检查环境变量
echo "检查环境变量..."
if [ -z "$OPENAI_API_KEY" ]; then
    echo "警告: OPENAI_API_KEY 环境变量未设置"
    echo "请设置: export OPENAI_API_KEY=your_api_key"
fi

if [ -z "$GITHUB_TOKEN" ]; then
    echo "提示: GITHUB_TOKEN 环境变量未设置 (可选)"
    echo "如需GitHub功能，请设置: export GITHUB_TOKEN=your_token"
fi

echo ""
echo "=== 启动应用 ==="
echo "使用Gradle启动应用（启用Java 24预览特性）..."

# 启动应用
./gradlew bootRun --args='--enable-preview'