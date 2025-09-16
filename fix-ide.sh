#!/bin/bash

echo "=== 修复IDE源码目录配置 ==="

# 创建必要的.idea目录
mkdir -p .idea

echo "✅ 创建IDE配置文件完成"

echo ""
echo "=== 手动修复步骤 ==="
echo "1. 在IntelliJ IDEA中打开项目"
echo "2. 右键点击项目根目录 -> Mark Directory as -> Unmark as Sources Root (如果java目录被标记为源码根目录)"
echo "3. 右键点击 src/main/java -> Mark Directory as -> Sources Root" 
echo "4. 右键点击 src/main/resources -> Mark Directory as -> Resources Root"
echo "5. 右键点击 src/test/java -> Mark Directory as -> Test Sources Root"
echo ""
echo "或者："
echo "1. File -> Project Structure -> Modules"
echo "2. 删除错误的源码路径"
echo "3. 添加正确的源码路径："
echo "   - Sources: src/main/java"
echo "   - Resources: src/main/resources" 
echo "   - Test Sources: src/test/java"
echo ""
echo "=== 验证修复 ==="
echo "修复后，包结构应该显示为："
echo "com.kami.springai.mcp"
echo "而不是 springai.mcp 或其他错误的包名"