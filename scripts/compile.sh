#!/bin/bash

echo "🔨 编译聊天室项目..."
echo "📁 新目录结构："
echo "   ├── core/       - 核心基础设施"
echo "   ├── messaging/  - 消息处理"
echo "   ├── client/     - 客户端模块"
echo "   └── server/     - 服务器模块"
echo ""

# 获取脚本所在目录并切换到项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# 创建编译输出目录
mkdir -p build

# 清理旧的编译文件
rm -rf build/*

# 编译所有Java文件 - 按依赖顺序编译
echo "🔄 正在编译..."
javac -encoding UTF-8 -d build -cp src \
    src/core/*.java \
    src/messaging/*.java \
    src/client/*.java \
    src/server/*.java

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 编译成功！"
    echo ""
    echo "📋 文件统计："
    echo "   - Java 源文件: $(find src -name "*.java" | wc -l | tr -d ' ') 个"
    echo "   - 编译后文件: $(find build -name "*.class" | wc -l | tr -d ' ') 个"
    echo ""
    echo "🚀 运行方式："
    echo "   1. 启动服务器: ./scripts/run_server.sh"
    echo "   2. 启动客户端: ./scripts/run_client.sh"
    echo ""
    echo "📝 手动运行："
    echo "   - 服务器: java -cp build server.ChatServer"
    echo "   - 客户端: java -cp build client.ChatClient"
else
    echo ""
    echo "❌ 编译失败！请检查代码错误"
    exit 1
fi 