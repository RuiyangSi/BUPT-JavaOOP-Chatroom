/*
 * =============================================================
 *  文件：ClientHandler.java  (服务器端单客户端处理线程)
 * -------------------------------------------------------------
 *  线程贯穿客户端生命周期：
 *    • 阶段 1: 验证登录 —— handleLogin()
 *    • 阶段 2: 消息循环   —— handleMessages()
 *    • 阶段 3: 清理关闭   —— close()
 *
 *  每收到 Message 后 switch(type) 分派：聊天、私聊、系统命令等，
 *  复杂逻辑委托 ChatServer 单例进行广播或路由。
 *
 *  🚦 线程退出条件：
 *    - 客户端主动发送 LOGOUT 或 quit 命令；
 *    - Socket 异常导致 readObject() 抛错；
 *
 *  所有异常均通过 server.log 记录便于运维排查。
 * =============================================================
 */
package server;

import core.Message;
import core.Logger;
import java.io.*;
import java.net.Socket;

/**
 * 客户端处理器 - 处理单个客户端的连接和消息
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ChatServer server;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;
    private boolean running;
    private Logger logger;
    
    /**
     * 构造函数
     *
     * @param clientSocket 与客户端建立的套接字
     * @param server       服务器核心对象，用于调用共享逻辑
     */
    public ClientHandler(Socket clientSocket, ChatServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.running = true;
        this.logger = Logger.getInstance();
    }
    
    /**
     * 线程入口：
     * <ol>
     *   <li>初始化 Object 流；</li>
     *   <li>调用 {@link #handleLogin()} 校验用户；</li>
     *   <li>若登录成功则进入 {@link #handleMessages()} 主循环。</li>
     * </ol>
     */
    @Override
    public void run() {
        try {
            // 初始化输入输出流
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());
            
            // 处理用户登录
            if (handleLogin()) {
                // 登录成功，开始处理消息
                handleMessages();
            }
        } catch (IOException e) {
            logger.error("客户端连接出错: %s", e.getMessage());
        } finally {
            close();
        }
    }
    
    /**
     * 读取并验证登录消息 (阻塞)
     *
     * @return true 表示登录成功并已向客户端发送 LOGIN_SUCCESS
     */
    private boolean handleLogin() {
        try {
            while (running) {
                Message loginMsg = (Message) input.readObject();
                
                if (Message.LOGIN.equals(loginMsg.getType())) {
                    String[] credentials = loginMsg.getContent().split(":");
                    if (credentials.length == 2) {
                        String username = credentials[0];
                        String password = credentials[1];
                        String clientIP = clientSocket.getInetAddress().getHostAddress();
                        
                        if (server.authenticateUser(username, password, clientIP)) {
                            this.username = username;
                            server.addOnlineClient(username, this);
                            
                            // 发送登录成功消息
                            Message successMsg = new Message(Message.LOGIN_SUCCESS, "系统", "登录成功！欢迎进入聊天室！");
                            sendMessage(successMsg);
                            return true;
                        } else {
                            // 发送登录失败消息
                            Message failMsg = new Message(Message.LOGIN_FAILED, "系统", "用户名或密码错误，请重试");
                            sendMessage(failMsg);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.info("客户端在登录阶段断开连接: %s", 
                       e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "连接已断开"));
        } catch (ClassNotFoundException e) {
            logger.error("登录消息反序列化失败: %s", e.getMessage());
        }
        return false;
    }
    
    /**
     * 主消息循环
     * <p>根据 {@link Message#getType()} 分派处理。</p>
     */
    private void handleMessages() {
        try {
            while (running) {
                Message message = (Message) input.readObject();
                
                // 检查消息是否为null
                if (message == null) {
                    logger.warn("收到空消息，忽略");
                    continue;
                }
                
                // 检查消息类型是否为null
                if (message.getType() == null) {
                    logger.warn("收到无类型消息，忽略");
                    continue;
                }
                
                switch (message.getType()) {
                    case Message.CHAT:
                        // 广播消息
                        if (message.getContent() != null && !message.getContent().trim().isEmpty()) {
                            server.broadcastMessage(message);
                        }
                        break;
                        
                    case Message.PRIVATE_CHAT:
                        // 私聊消息
                        if (message.getContent() != null && !message.getContent().trim().isEmpty()) {
                            server.sendPrivateMessage(message);
                        }
                        break;
                        
                    case Message.SYSTEM_COMMAND:
                        // 系统命令
                        if (message.getContent() != null) {
                            handleSystemCommand(message);
                        }
                        break;
                        
                    case Message.LOGOUT:
                        // 用户退出
                        running = false;
                        break;
                        
                    default:
                        logger.warn("收到未知消息类型: %s", message.getType());
                }
            }
        } catch (IOException e) {
            if (running) {
                // 客户端连接断开是常见情况，提供更详细的错误信息
                String errorDetail = e.getClass().getSimpleName() + ": " + 
                                   (e.getMessage() != null ? e.getMessage() : "连接已断开");
                logger.info("客户端连接断开: %s (用户: %s)", errorDetail, username != null ? username : "未知");
            }
        } catch (ClassNotFoundException e) {
            logger.error("消息反序列化失败: %s (用户: %s)", e.getMessage(), username != null ? username : "未知");
        } catch (Exception e) {
            logger.error("处理消息时发生意外错误: %s (用户: %s)", 
                        e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "未知异常"), 
                        username != null ? username : "未知");
            e.printStackTrace();
        }
    }
    
    /**
     * 执行系统命令，如 list / quit / anonymous
     *
     * @param message SYSTEM_COMMAND 类型消息
     */
    private void handleSystemCommand(Message message) {
        String command = message.getContent().toLowerCase();
        Message response;
        
        switch (command) {
            case "list":
                response = new Message(Message.SYSTEM_RESPONSE, "系统", server.getOnlineUsersList());
                sendMessage(response);
                break;
                
            case "quit":
                response = new Message(Message.SYSTEM_RESPONSE, "系统", "再见！您已退出聊天室。");
                sendMessage(response);
                running = false;
                break;
                
            case "showanonymous":
                // 这个功能需要客户端支持，服务器只转发状态
                response = new Message(Message.SYSTEM_RESPONSE, "系统", "匿名状态查询请求");
                sendMessage(response);
                break;
                
            case "anonymous":
                // 切换匿名状态
                response = new Message(Message.ANONYMOUS_TOGGLE, "系统", "匿名状态已切换");
                sendMessage(response);
                break;
                
            default:
                response = new Message(Message.SYSTEM_RESPONSE, "系统", "未知命令: " + command);
                sendMessage(response);
        }
    }
    
    /**
     * 将序列化 Message 写回客户端
     *
     * @param message 要发送的消息对象
     */
    public void sendMessage(Message message) {
        try {
            if (output != null) {
                output.writeObject(message);
                output.flush();
            }
        } catch (IOException e) {
            logger.error("发送消息失败: %s", e.getMessage());
            close();
        }
    }
    
    /**
     * 主动或被动关闭客户端连接，并从在线列表移除
     */
    public void close() {
        running = false;
        
        if (username != null) {
            server.removeOnlineClient(username);
        }
        
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.error("关闭客户端连接时出错: %s", e.getMessage());
        }
    }
    
    /**
     * 获取用户名
     *
     * @return 登录成功后分配的用户名，可能为 null（未登录通过）
     */
    public String getUsername() {
        return username;
    }
} 