#!/bin/bash

echo "💻 启动智能聊天室客户端..."

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
        echo "❌ 编译失败，无法启动客户端"
        exit 1
    fi
fi

# 检查关键类文件是否存在
if [ ! -f "build/client/ChatClient.class" ]; then
    echo "❌ 客户端类文件不存在，正在重新编译..."
    "$SCRIPT_DIR/compile.sh"
    if [ $? -ne 0 ]; then
        echo "❌ 编译失败，无法启动客户端"
        exit 1
    fi
fi

echo "✅ 准备启动客户端..."
echo "🔗 连接信息:"
echo "   - 服务器地址: localhost:8080"
echo "   - 默认用户: alice, bob, admin 等"
echo ""
echo "💡 使用提示:"
echo "   - 普通聊天: 直接输入消息"
echo "   - 私聊: @用户名 消息内容"
echo "   - 系统命令: @@命令"
echo "     * @@list - 查看在线用户"
echo "     * @@anonymous - 切换匿名模式"
echo "     * @@showanonymous - 查看匿名状态"
echo "     * @@quit - 退出聊天室"
echo ""
echo "🚀 启动中..."

# 启动客户端
java -cp build client.ChatClient 