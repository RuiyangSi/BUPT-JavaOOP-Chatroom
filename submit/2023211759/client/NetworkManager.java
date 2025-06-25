/*
 * ================================================================
 *  文件：NetworkManager.java   (网络连接管理器)
 * ---------------------------------------------------------------
 *  职责：
 *   • 管理与服务器的TCP连接建立和断开；
 *   • 处理消息的发送和接收；
 *   • 管理网络IO线程；
 *   • 提供连接状态监控。
 *
 *  设计原则：
 *   - 单一职责：只负责网络通信
 *   - 事件驱动：通过MessageHandler接口回调
 *   - 线程安全：内部处理多线程问题
 * ================================================================
 */
package client;

import core.Message;
import javax.swing.SwingUtilities;
import java.io.*;
import java.net.Socket;

/**
 * 网络连接管理器 - 负责客户端的网络通信
 */
public class NetworkManager {
    
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private boolean connected = false;
    private boolean isQuitting = false;
    private Thread receiveThread;
    private MessageHandler messageHandler;
    
    /**
     * 消息处理接口 - 用于回调处理接收到的消息
     */
    public interface MessageHandler {
        /**
         * 处理接收到的消息
         * @param message 服务器发送的消息
         */
        void handleMessage(Message message);
        
        /**
         * 处理连接错误
         * @param error 错误信息
         * @param isQuitting 是否正在主动退出
         */
        void handleConnectionError(String error, boolean isQuitting);
    }
    
    /**
     * 构造函数
     * @param messageHandler 消息处理器
     */
    public NetworkManager(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }
    
    /**
     * 连接到默认服务器
     * @return true 表示连接成功
     */
    public boolean connect() {
        return connect(DEFAULT_HOST, DEFAULT_PORT);
    }
    
    /**
     * 连接到指定服务器
     * @param host 服务器地址
     * @param port 服务器端口
     * @return true 表示连接成功
     */
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            connected = true;
            
            // 启动消息接收线程
            startReceiveThread();
            
            return true;
        } catch (IOException e) {
            notifyConnectionError("无法连接到服务器: " + e.getMessage(), false);
            return false;
        }
    }
    
    /**
     * 启动消息接收线程
     */
    private void startReceiveThread() {
        receiveThread = new Thread(this::receiveMessages, "NetworkReceiveThread");
        receiveThread.setDaemon(false); // 确保线程能正常完成
        receiveThread.start();
    }
    
    /**
     * 消息接收线程主循环
     */
    private void receiveMessages() {
        try {
            while (connected && !Thread.currentThread().isInterrupted()) {
                Message message = (Message) input.readObject();
                
                if (message != null) {
                    // 在EDT线程中处理消息
                    SwingUtilities.invokeLater(() -> {
                        if (messageHandler != null) {
                            messageHandler.handleMessage(message);
                        }
                    });
                } else {
                    System.out.println("收到空消息，忽略");
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (connected && !isQuitting) {
                // 连接异常断开
                SwingUtilities.invokeLater(() -> {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "未知连接错误";
                    notifyConnectionError("与服务器连接断开: " + errorMsg, false);
                });
            } else if (isQuitting) {
                // 正常退出
                System.out.println("正常退出聊天室，连接已断开");
            }
        } catch (Exception e) {
            System.out.println("接收消息时发生意外错误: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            e.printStackTrace();
        }
    }
    
    /**
     * 发送消息到服务器
     * @param message 要发送的消息
     * @return true 表示发送成功
     */
    public boolean sendMessage(Message message) {
        if (!connected || output == null) {
            return false;
        }
        
        try {
            output.writeObject(message);
            output.flush();
            return true;
        } catch (IOException e) {
            // 如果正在退出，不显示错误消息
            if (!isQuitting) {
                notifyConnectionError("发送消息失败: " + e.getMessage(), false);
            } else {
                // 正在退出，连接断开是正常的
                System.out.println("退出过程中连接断开，这是正常的");
            }
            return false;
        }
    }
    
    /**
     * 设置退出标志（用于优雅退出）
     */
    public void setQuitting(boolean quitting) {
        this.isQuitting = quitting;
    }
    
    /**
     * 断开连接
     * @param sendLogoutMessage 是否发送登出消息
     */
    public void disconnect(boolean sendLogoutMessage) {
        if (connected && sendLogoutMessage) {
            // 发送登出消息
            Message logoutMsg = new Message(Message.LOGOUT, "", "用户退出");
            sendMessage(logoutMsg);
        }
        
        connected = false;
        
        // 中断接收线程
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
        }
        
        // 关闭网络资源
        closeNetworkResources();
    }
    
    /**
     * 断开连接（默认发送登出消息）
     */
    public void disconnect() {
        disconnect(true);
    }
    
    /**
     * 关闭网络资源
     */
    private void closeNetworkResources() {
        try {
            if (input != null) {
                input.close();
                input = null;
            }
        } catch (IOException e) {
            // 忽略关闭错误
        }
        
        try {
            if (output != null) {
                output.close();
                output = null;
            }
        } catch (IOException e) {
            // 忽略关闭错误
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            // 忽略关闭错误
        }
    }
    
    /**
     * 通知连接错误
     */
    private void notifyConnectionError(String error, boolean isQuitting) {
        if (messageHandler != null) {
            messageHandler.handleConnectionError(error, isQuitting);
        }
    }
    
    /**
     * 检查是否已连接
     * @return true 表示已连接
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    /**
     * 获取连接状态描述
     * @return 连接状态字符串
     */
    public String getConnectionStatus() {
        if (isConnected()) {
            return "已连接到服务器";
        } else {
            return "未连接";
        }
    }
} 