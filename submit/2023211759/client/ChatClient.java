/*
 * =====================================================================
 *  文件：ChatClient.java   (客户端主逻辑 + 连接层)
 * ---------------------------------------------------------------------
 *  该类负责：
 *    1. 建立与服务器 (localhost:8080) 的 Socket 连接；
 *    2. 完成登录 / 登出 / 保持长连；
 *    3. 启动专用线程接收服务器推送的 Message 对象；
 *    4. 将接收到的消息分门别类地转交 GUI (ChatFrame) 显示；
 *    5. 同时充当 GUI 的"控制器"—— 将用户操作(发送消息/系统命令)序列化后发往服务器；
 *    6. 维护客户端运行期状态：用户名、匿名开关、在线用户列表等。
 *
 *  ⚠️ 线程模型：
 *    • GUI 事件 -> Swing EDT 线程；
 *    • 网络接收 -> 自建 Receive Thread；
 *      为避免线程安全问题，所有 UI 更新都使用 SwingUtilities.invokeLater() 切回 EDT。
 *
 *  🚪 退出流程 (@@quit)：
 *    a) 用户在输入框输入 @@quit，或点击关闭窗口；
 *    b) sendSystemCommand() 发现 quit，立即 isQuitting=true 并发送 Message;
 *    c) 服务器 handleSystemCommand -> 返回 "再见…退出" 并主动 close；
 *    d) 客户端 receiveMessages() 捕获 Socket 关闭:
 *          如果 isQuitting == true 则视为正常，不弹框。
 *          并调用 logoutToLogin() 回到登录界面；
 *
 *  扩展提示：
 *    若需要支持文件传输或图片，可在 Message 增加 byte[] payload 并在此处处理。
 * =====================================================================
 */
package client;

import core.Message;
import messaging.MessageParser;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 聊天客户端 - 智能功能版本
 */
public class ChatClient implements NetworkManager.MessageHandler {
    
    private NetworkManager networkManager;
    private String username;
    private boolean anonymous = false;
    
    // GUI组件
    private LoginFrame loginFrame;
    private ChatFrame chatFrame;
    
    /**
     * 构造函数：启动 GUI 初始化流程
     */
    public ChatClient() {
        networkManager = new NetworkManager(this);
        initializeGUI();
    }
    
    /**
     * 初始化登录界面等 GUI 组件；该方法应始终在 EDT 线程调用
     */
    private void initializeGUI() {
        // 使用默认的Swing外观
        loginFrame = new LoginFrame(this);
        loginFrame.setVisible(true);
    }
    
    /**
     * 与服务器建立 TCP 连接，并创建对象流
     *
     * @return true 表示连接成功
     */
    public boolean connectToServer() {
        return networkManager.connect();
    }
    
    /**
     * 连接到指定服务器
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @return true 表示连接成功
     */
    public boolean connectToServer(String host, int port) {
        return networkManager.connect(host, port);
    }
    
    /**
     * 尝试向服务器发送登录消息
     *
     * @param username 用户名
     * @param password 密码
     */
    public void attemptLogin(String username, String password) {
        if (!networkManager.isConnected() && !connectToServer()) {
            return;
        }
        
        Message loginMsg = new Message(Message.LOGIN, username, username + ":" + password);
        if (!networkManager.sendMessage(loginMsg)) {
            JOptionPane.showMessageDialog(loginFrame, "发送登录信息失败", 
                                        "网络错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 尝试向指定服务器发送登录消息
     *
     * @param username 用户名
     * @param password 密码
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public void attemptLogin(String username, String password, String host, int port) {
        if (!networkManager.isConnected() && !connectToServer(host, port)) {
            return;
        }
        
        Message loginMsg = new Message(Message.LOGIN, username, username + ":" + password);
        if (!networkManager.sendMessage(loginMsg)) {
            JOptionPane.showMessageDialog(loginFrame, "发送登录信息失败", 
                                        "网络错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ==================== NetworkManager.MessageHandler 接口实现 ====================
    
    /**
     * 处理接收到的消息（NetworkManager.MessageHandler接口实现）
     */
    @Override
    public void handleMessage(Message message) {
        handleServerMessage(message);
    }
    
    /**
     * 处理连接错误（NetworkManager.MessageHandler接口实现）
     */
    @Override
    public void handleConnectionError(String error, boolean isQuitting) {
        if (!isQuitting) {
            JOptionPane.showMessageDialog(null, error, "连接错误", JOptionPane.ERROR_MESSAGE);
            disconnect();
        } else {
            // 正在执行quit操作，连接断开是预期行为
            System.out.println("正常退出聊天室，连接已断开");
            logoutToLogin();
        }
    }
    
    /**
     * 根据服务器消息类型分派，更新 UI 或本地状态
     *
     * @param message 服务器推送的消息
     */
    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            // 检查消息和消息类型是否为null
            if (message == null || message.getType() == null) {
                System.out.println("收到无效消息，忽略");
                return;
            }
            
            switch (message.getType()) {
                case Message.LOGIN_SUCCESS:
                    // 登录成功
                    loginFrame.setVisible(false);
                    chatFrame = new ChatFrame(this, username);
                    chatFrame.setVisible(true);
                    chatFrame.displayMessage("系统", message.getContent(), false);
                    // 请求在线用户列表
                    sendSystemCommand("list");
                    break;
                    
                case Message.LOGIN_FAILED:
                    // 登录失败
                    JOptionPane.showMessageDialog(loginFrame, message.getContent(), 
                                                "登录失败", JOptionPane.ERROR_MESSAGE);
                    break;
                    
                case Message.CHAT:
                    // 普通聊天消息
                    if (chatFrame != null) {
                        String senderName = message.isAnonymous() ? "匿名用户" : message.getSender();
                        chatFrame.displayMessage(senderName, message.getContent(), false);
                    }
                    break;
                    
                case Message.PRIVATE_CHAT:
                    // 私聊消息
                    if (chatFrame != null) {
                        String senderName = message.isAnonymous() ? "🎭 匿名用户" : message.getSender();
                        boolean isFromMe = message.getSender().equals(username);
                        String prefix;
                        if (isFromMe) {
                            prefix = "💬 " + username + "(我) ➤ " + message.getReceiver();
                        } else {
                            prefix = "💌 " + senderName;
                        }
                        chatFrame.displayMessage(prefix, message.getContent(), true);
                    }
                    break;
                    
                case Message.SYSTEM_RESPONSE:
                    // 系统响应
                    if (chatFrame != null) {
                        String content = message.getContent();
                        if (content.contains("在线用户列表：")) {
                            // 解析在线用户列表
                            parseOnlineUsers(content);
                        }
                        chatFrame.displayMessage("系统", content, false);
                        
                        // 检查是否是quit命令的响应
                        if (content.contains("再见") && content.contains("退出")) {
                            // 立即设置退出标志，避免异常处理中的程序退出
                            networkManager.setQuitting(true);
                            // 延迟1.5秒后返回登录页面，让用户看到退出消息
                            Timer timer = new Timer(1500, e -> logoutToLogin());
                            timer.setRepeats(false);
                            timer.start();
                        }
                    }
                    break;
                    
                case Message.ANONYMOUS_TOGGLE:
                    // 匿名状态切换
                    anonymous = !anonymous;
                    if (chatFrame != null) {
                        chatFrame.updateAnonymousStatus(anonymous);
                        String status = anonymous ? "已开启匿名聊天" : "已关闭匿名聊天";
                        chatFrame.displayMessage("系统", status, false);
                    }
                    break;
                    
                case Message.USER_LIST_UPDATE:
                    // 用户列表更新广播
                    if (chatFrame != null) {
                        parseUserListData(message.getContent());
                    }
                    break;
            }
        });
    }
    
    /**
     * 解析形如 "在线用户列表：\n- user1\n- user2" 的响应文本
     */
    private void parseOnlineUsers(String content) {
        List<String> users = new ArrayList<>();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            if (line.startsWith("- ")) {
                String user = line.substring(2).trim();
                users.add(user);
            }
        }
        
        if (chatFrame != null) {
            chatFrame.updateOnlineUsers(users);
        }
    }
    
    /**
     * 解析 USER_LIST_UPDATE 广播中逗号分隔的用户名
     */
    private void parseUserListData(String data) {
        List<String> users = new ArrayList<>();
        if (data != null && !data.trim().isEmpty()) {
            String[] userArray = data.split(",");
            for (String user : userArray) {
                if (!user.trim().isEmpty()) {
                    users.add(user.trim());
                }
            }
        }
        
        if (chatFrame != null) {
            chatFrame.updateOnlineUsers(users);
        }
    }
    
    /**
     * 将输入框文本封装为 Message 并发送；支持广播、私聊、系统命令
     *
     * @param content 输入框原始文本
     */
    public void sendChatMessage(String content) {
        if (!networkManager.isConnected()) {
            return;
        }
        
        // 使用MessageParser解析输入
        MessageParser.ParseResult parseResult = MessageParser.parseInput(content, username);
        
        if (!parseResult.isValid()) {
            // 显示解析错误
            if (chatFrame != null) {
                chatFrame.displayMessage("系统", parseResult.getErrorMessage(), false);
            }
            return;
        }
        
        // 处理本地命令
        if (parseResult.getType() == MessageParser.MessageType.LOCAL_COMMAND) {
            handleLocalCommand(parseResult.getCommand());
            return;
        }
        
        // 处理系统命令
        if (parseResult.getType() == MessageParser.MessageType.SYSTEM_COMMAND) {
            sendSystemCommand(parseResult.getCommand());
            return;
        }
        
        // 创建消息对象
        Message message = MessageParser.createMessage(parseResult, username, anonymous);
        
        if (message == null) {
            if (chatFrame != null) {
                chatFrame.displayMessage("系统", "创建消息失败", false);
            }
            return;
        }
        
        // 发送消息
        if (!networkManager.sendMessage(message)) {
            if (chatFrame != null) {
                chatFrame.displayMessage("系统", "发送消息失败", false);
            }
        }
    }
    
    /**
     * 处理本地命令（不发送到服务器）
     * 
     * @param command 本地命令
     */
    private void handleLocalCommand(String command) {
        if ("showanonymous".equalsIgnoreCase(command.trim())) {
            String status = anonymous ? "当前为匿名模式 (🎭)" : "当前为实名模式 (👤)";
            if (chatFrame != null) {
                chatFrame.displayMessage("系统", status, false);
            }
        }
    }
    
    /**
     * 发送系统级命令到服务器
     *
     * @param command list / quit / anonymous
     */
    private void sendSystemCommand(String command) {
        if (!networkManager.isConnected()) return;
        
        // 如果是 quit 命令，提前标记正在退出，避免收到连接断开异常时弹窗
        if ("quit".equalsIgnoreCase(command.trim())) {
            networkManager.setQuitting(true);
            
            // 发送quit消息
            Message message = new Message(Message.SYSTEM_COMMAND, username, command);
            boolean sent = networkManager.sendMessage(message);
            
            // 无论发送是否成功，都执行退出逻辑
            if (!sent) {
                // 如果发送失败（连接已断开），直接返回登录页面
                SwingUtilities.invokeLater(() -> {
                    if (chatFrame != null) {
                        chatFrame.displayMessage("系统", "连接已断开，正在返回登录界面...", false);
                    }
                    Timer timer = new Timer(1000, e -> logoutToLogin());
                    timer.setRepeats(false);
                    timer.start();
                });
            }
        } else {
            // 非quit命令，正常发送
            Message message = new Message(Message.SYSTEM_COMMAND, username, command);
            networkManager.sendMessage(message);
        }
    }
    
    /**
     * 断开连接并结束 JVM
     */
    public void disconnect() {
        disconnect(true);
    }
    
    /**
     * 内部断开逻辑
     *
     * @param exitProgram true 则调用 System.exit
     */
    public void disconnect(boolean exitProgram) {
        networkManager.disconnect();
        
        if (exitProgram) {
            System.exit(0);
        }
    }
    
    /**
     * 从聊天室返回登录页面（不会退出程序）
     */
    public void logoutToLogin() {
        // 关闭聊天窗口
        if (chatFrame != null) {
            chatFrame.setVisible(false);
            chatFrame.dispose();
            chatFrame = null;
        }
        
        // 断开连接但不退出程序
        disconnect(false);
        
        // 重置状态
        username = null;
        anonymous = false;
        networkManager.setQuitting(false);
        
        // 显示登录窗口
        SwingUtilities.invokeLater(() -> {
            if (loginFrame == null) {
                loginFrame = new LoginFrame(this);
            }
            // 确保窗口在前台显示
            loginFrame.setVisible(true);
            loginFrame.toFront();
            loginFrame.requestFocus();
        });
    }
    
    /**
     * 获取当前匿名状态
     */
    public boolean isAnonymous() {
        return anonymous;
    }
    
    /**
     * 设置当前用户名（仅在登录前调用）
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * 应用入口；启动 Swing EDT 并创建 ChatClient
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient());
    }
} 