#!/bin/bash

echo "🖥️  启动聊天室服务器..."

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 切换到项目根目录
cd "$PROJECT_ROOT"

# 检查是否已编译
if [ ! -d "build" ]; then
    echo "❌ 编译文件不存在，正在自动编译..."
    "$SCRIPT_DIR/compile.sh"
    if [ $? -ne 0 ]; then
        echo "❌ 编译失败，无法启动服务器"
        exit 1
    fi
fi

# 检查关键类文件是否存在
if [ ! -f "build/server/ChatServer.class" ]; then
    echo "❌ 服务器类文件不存在，正在重新编译..."
    "$SCRIPT_DIR/compile.sh"
    if [ $? -ne 0 ]; then
        echo "❌ 编译失败，无法启动服务器"
        exit 1
    fi
fi

echo "✅ 准备启动服务器..."
echo "📡 监听端口: 8080"
echo "👥 支持的功能:"
echo "   - 多用户聊天"
echo "   - 私聊消息 (@用户名 消息)"
echo "   - 匿名模式 (@@anonymous)"
echo "   - 用户列表 (@@list)"
echo ""
echo "🎮 服务器命令:"
echo "   - list: 查看在线用户"
echo "   - listall: 查看所有用户"
echo "   - quit: 关闭服务器"
echo ""
echo "🚀 启动中..."

# 启动服务器
java -cp build server.ChatServer 