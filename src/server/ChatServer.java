/*
 * ================================================================
 *  文件：ChatServer.java   (服务器主入口)
 * ---------------------------------------------------------------
 *  功能概览：
 *   1) 初始化用户库(users.txt)并监听 8080 端口；
 *   2) 为每个连接创建 ClientHandler 线程处理业务；
 *   3) 支持服务器控制台命令：list, listall, quit；
 *   4) 负责广播 / 私聊转发 / 用户列表更新等逻辑；
 *   5) 将运行日志写入 server.log。
 *
 *  ⚙️ 技术要点：
 *   • 使用阻塞式 ServerSocket + 每客户端一线程；
 *   • 采用 synchronized Map 来维护在线用户，确保线程安全；
 *   • shutdown() 过程中先通知所有 ClientHandler 关闭，再关闭 socket；
 *
 *  📌 扩展建议：
 *   - 若并发客户端数增多，可改为 NIO + Selector 实现；
 *   - 如需持久化聊天记录，可在 broadcastMessage() 写入数据库或文件。
 * ================================================================
 */
package server;

import core.Message;
import core.User;
import core.Logger;
import messaging.MessageBroadcaster;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天服务器主类
 */
public class ChatServer implements UserManager.UserStatusListener {
    private static final int PORT = 8080;
    private static final String USER_FILE = "data/users.txt";
    
    private ServerSocket serverSocket;
    private UserManager userManager;                       // 用户管理器
    private MessageBroadcaster messageBroadcaster;         // 消息广播器
    private boolean running;
    private Logger logger;
    
    /**
     * 构造函数
     * <p>1) 初始化用户管理器<br>
     *    2) 配置日志器<br>
     *    3) 设置用户状态监听器
     * </p>
     */
    public ChatServer() {
        // 配置日志器
        logger = Logger.getInstance();
        logger.config(
            Logger.Level.INFO,     // 最小日志级别
            "server.log",          // 日志文件名
            true,                  // 启用控制台输出
            true                   // 启用文件输出
        );
        
        // 初始化用户管理器
        userManager = new UserManager(USER_FILE);
        userManager.setUserStatusListener(this);
        
        // 初始化消息广播器
        messageBroadcaster = new MessageBroadcaster(userManager);
    }
    
    // ==================== UserManager.UserStatusListener 接口实现 ====================
    
    /**
     * 用户上线事件处理
     */
    @Override
    public void onUserOnline(String username) {
        // 广播用户列表更新
        messageBroadcaster.broadcastUserListUpdate();
    }
    
    /**
     * 用户下线事件处理
     */
    @Override
    public void onUserOffline(String username) {
        // 广播用户列表更新
        messageBroadcaster.broadcastUserListUpdate();
    }
    
    /**
     * 写日志信息
     *
     * @param message 日志文本
     */
    public void log(String message) {
        logger.info(message);
    }
    
    /**
     * 启动服务器主循环
     *
     * <p>1) 创建 {@link ServerSocket} 并标记 running=true<br>
     *    2) 新线程执行 {@link #handleServerCommands()} 监听控制台指令<br>
     *    3) 进入阻塞 accept() 循环，收到连接即分配 {@link ClientHandler} 线程
     * </p>
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            log("服务器启动，监听端口: " + PORT);
            
            // 启动命令行输入线程
            new Thread(this::handleServerCommands).start();
            
            // 主循环：接受客户端连接
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    new Thread(clientHandler).start();
                    log("新客户端连接: " + clientSocket.getInetAddress().getHostAddress());
                } catch (IOException e) {
                    if (running) {
                        log("接受客户端连接时出错: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log("服务器启动失败: " + e.getMessage());
        }
    }
    
    /**
     * 服务器控制台命令处理线程
     *
     * 支持命令：list / listall / quit
     */
    private void handleServerCommands() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("服务器命令：list(列出在线用户), listall(列出所有用户), quit(退出)");
        
        while (running) {
            System.out.print("server> ");
            String command = scanner.nextLine().trim();
            
            switch (command.toLowerCase()) {
                case "list":
                    System.out.println("在线用户 (" + userManager.getOnlineUserCount() + "):");
                    for (String username : userManager.getOnlineUsernames()) {
                        System.out.println("  - " + username);
                    }
                    break;
                    
                case "listall":
                    System.out.println("所有用户 (" + userManager.getTotalUserCount() + "):");
                    for (User user : userManager.getAllUsers()) {
                        String status = user.isOnline() ? "在线" : "离线";
                        System.out.println("  - " + user.getUsername() + " (" + status + ")");
                    }
                    break;
                    
                case "quit":
                    shutdown();
                    break;
                    
                default:
                    System.out.println("未知命令: " + command);
            }
        }
        scanner.close();
    }
    
    /**
     * 校验用户凭证
     *
     * @param username 用户名
     * @param password 密码
     * @param clientIP 客户端 IP，用于日志
     * @return true 表示通过验证
     */
    public boolean authenticateUser(String username, String password, String clientIP) {
        return userManager.authenticateUser(username, password, clientIP);
    }
    
    /**
     * 将用户标记为在线并保存其处理器
     *
     * @param username 用户名
     * @param handler  对应的 {@link ClientHandler}
     */
    public synchronized void addOnlineClient(String username, ClientHandler handler) {
        userManager.addOnlineUser(username, handler);
    }
    
    /**
     * 将用户从在线 Map 中移除
     *
     * @param username 用户名
     */
    public synchronized void removeOnlineClient(String username) {
        userManager.removeOnlineUser(username);
    }
    
    /**
     * 广播文本/私聊/系统消息给所有在线客户端
     *
     * @param message 消息载体
     */
    public void broadcastMessage(Message message) {
        messageBroadcaster.broadcastToAll(message);
    }
    
    /**
     * 将消息发送到指定接收者，同时回显给发送者
     *
     * @param message 私聊消息
     */
    public void sendPrivateMessage(Message message) {
        messageBroadcaster.sendPrivateMessage(message);
    }
    
    /**
     * 构造人类可读的在线用户列表
     *
     * @return 字符串格式用户清单，用于 SYSTEM_RESPONSE
     */
    public String getOnlineUsersList() {
        return userManager.getOnlineUsersListText();
    }
    

    
    /**
     * 优雅关闭服务器：
     * <ul><li>停止主循环<li>关闭所有客户端连接<li>关闭 ServerSocket<li>关闭用户管理器<li>关闭日志器</ul>
     */
    public void shutdown() {
        running = false;
        log("服务器关闭中...");
        
        // 通知所有客户端服务器关闭
        for (ClientHandler handler : userManager.getAllOnlineHandlers()) {
            handler.close();
        }
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("关闭服务器套接字时出错: " + e.getMessage());
        }
        
        // 关闭用户管理器
        userManager.shutdown();
        
        log("服务器已关闭");
        logger.shutdown();
        System.exit(0);
    }
    
    /**
     * 入口函数
     */
    public static void main(String[] args) {
        new ChatServer().start();
    }
} 