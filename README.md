# 💬 Java聊天室系统

🚀 基于C/S架构的多用户聊天室系统，采用Java面向对象编程实现。

## ✨ 功能特性

- 🌐 **多用户并发聊天** - 支持至少5个用户同时在线
- 💬 **私聊功能** - 使用`@用户名 消息内容`格式发送私聊
- 🎭 **匿名模式** - 支持匿名聊天，保护用户隐私
- 📋 **用户管理** - 查看在线用户列表和用户状态
- 📝 **日志记录** - 完整的用户行为和系统事件日志
- 🖥️ **图形界面** - 基于Swing的友好用户界面

## 🚀 快速开始

### 📋 环境要求
- ☕ JDK 8或更高版本
- 🖥️ 支持GUI的操作系统

### 🔧 编译项目
```bash
# 编译所有源文件
./scripts/compile.sh
```

### 🖥️ 启动服务器
```bash
# macOS/Linux
./scripts/run_server.sh

# Windows
java -cp build server.ChatServer
```

### 💻 启动客户端
```bash
# macOS/Linux  
./scripts/run_client.sh

# Windows
java -cp build client.ChatClient
```

## 📖 使用说明

### 🔐 登录
- 启动客户端后在登录界面输入用户名和密码
- 可配置服务器地址和端口(默认127.0.0.1:8080)

### 💬 聊天命令
- **公开消息**: 直接输入文本
- **私聊消息**: `@用户名 消息内容`
- **系统命令**:
  - `@@list` - 📋 查看在线用户
  - `@@anonymous` - 🎭 切换匿名模式
  - `@@showanonymous` - 👀 查看匿名状态
  - `@@quit` - 🚪 退出聊天室

### ⚙️ 服务器管理
- `list` - 👥 查看当前在线用户
- `listall` - 📊 查看所有注册用户  
- `quit` - 🔴 关闭服务器

## 📁 项目结构

```
├── 📂 src/            # 源代码目录
│   ├── 🎯 core/           # 核心数据结构
│   │   ├── User.java
│   │   ├── Message.java
│   │   └── Logger.java
│   ├── 🖥️ server/         # 服务器模块
│   │   ├── ChatServer.java
│   │   ├── ClientHandler.java
│   │   └── UserManager.java
│   ├── 💻 client/         # 客户端模块
│   │   ├── ChatClient.java
│   │   ├── ChatFrame.java
│   │   ├── LoginFrame.java
│   │   └── NetworkManager.java
│   └── 📨 messaging/      # 消息处理模块
│       ├── MessageBroadcaster.java
│       └── MessageParser.java
├── 📂 scripts/        # 脚本文件
│   ├── compile.sh         # 编译脚本
│   ├── run_server.sh      # 服务器启动脚本
│   └── run_client.sh      # 客户端启动脚本
├── 📂 data/           # 数据文件
│   └── users.txt          # 用户账户数据
├── 📂 docs/           # 文档文件
│   ├── report.pdf         # 项目报告
│   └── 计算机_期末作业.docx
├── 📂 build/          # 编译输出目录 (被.gitignore忽略)
├── 📄 .gitignore      # Git忽略文件配置
└── 📄 README.md       # 项目说明文档
```
