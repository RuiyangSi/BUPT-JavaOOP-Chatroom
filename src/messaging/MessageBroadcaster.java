/*
 * ================================================================
 *  文件：MessageBroadcaster.java   (消息广播器)
 * ---------------------------------------------------------------
 *  职责：
 *   • 处理消息的广播分发；
 *   • 管理私聊消息的路由；
 *   • 处理用户列表更新广播；
 *   • 提供统一的消息分发接口。
 *
 *  设计原则：
 *   - 单一职责：只负责消息分发
 *   - 无状态：不保存消息或用户数据
 *   - 可扩展：易于添加新的广播类型
 * ================================================================
 */
package messaging;

import core.Message;
import core.Logger;
import server.ClientHandler;
import server.UserManager;
import java.util.Collection;
import java.util.Set;

/**
 * 消息广播器 - 负责服务器端的消息分发
 */
public class MessageBroadcaster {
    
    private final UserManager userManager;
    private final Logger logger;
    
    /**
     * 广播结果类 - 封装广播操作的结果
     */
    public static class BroadcastResult {
        private final boolean success;
        private final int recipientCount;
        private final String errorMessage;
        
        private BroadcastResult(boolean success, int recipientCount, String errorMessage) {
            this.success = success;
            this.recipientCount = recipientCount;
            this.errorMessage = errorMessage;
        }
        
        public static BroadcastResult success(int recipientCount) {
            return new BroadcastResult(true, recipientCount, null);
        }
        
        public static BroadcastResult failure(String errorMessage) {
            return new BroadcastResult(false, 0, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public int getRecipientCount() { return recipientCount; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 构造函数
     * @param userManager 用户管理器
     */
    public MessageBroadcaster(UserManager userManager) {
        this.userManager = userManager;
        this.logger = Logger.getInstance();
    }
    
    /**
     * 广播消息给所有在线用户
     * 
     * @param message 要广播的消息
     * @return 广播结果
     */
    public BroadcastResult broadcastToAll(Message message) {
        if (message == null) {
            return BroadcastResult.failure("消息不能为空");
        }
        
        Collection<ClientHandler> handlers = userManager.getAllOnlineHandlers();
        
        if (handlers.isEmpty()) {
            logger.warn("没有在线用户，无法广播消息");
            return BroadcastResult.success(0);
        }
        
        int successCount = 0;
        for (ClientHandler handler : handlers) {
            try {
                handler.sendMessage(message);
                successCount++;
            } catch (Exception e) {
                logger.warn("向客户端发送消息失败: " + e.getMessage());
            }
        }
        
        logger.info("广播消息: " + message.getSender() + " -> " + message.getContent() + 
                   " (发送给 " + successCount + "/" + handlers.size() + " 个用户)");
        
        return BroadcastResult.success(successCount);
    }
    
    /**
     * 发送私聊消息
     * 
     * @param message 私聊消息
     * @return 发送结果
     */
    public BroadcastResult sendPrivateMessage(Message message) {
        if (message == null) {
            return BroadcastResult.failure("消息不能为空");
        }
        
        if (message.getReceiver() == null || message.getReceiver().trim().isEmpty()) {
            return BroadcastResult.failure("接收者不能为空");
        }
        
        String receiverName = message.getReceiver();
        String senderName = message.getSender();
        
        ClientHandler receiver = userManager.getOnlineUserHandler(receiverName);
        ClientHandler sender = userManager.getOnlineUserHandler(senderName);
        
        if (receiver == null) {
            // 接收者不在线，通知发送者
            if (sender != null) {
                Message errorMsg = new Message(Message.SYSTEM_RESPONSE, "系统", 
                                             "用户 " + receiverName + " 不在线");
                try {
                    sender.sendMessage(errorMsg);
                } catch (Exception e) {
                    logger.warn("发送错误消息失败: " + e.getMessage());
                }
            }
            
            logger.warn("私聊失败: 用户 " + receiverName + " 不在线");
            return BroadcastResult.failure("用户 " + receiverName + " 不在线");
        }
        
        // 发送给接收者和发送者（用于确认）
        int successCount = 0;
        
        try {
            receiver.sendMessage(message);
            successCount++;
        } catch (Exception e) {
            logger.warn("向接收者发送私聊消息失败: " + e.getMessage());
        }
        
        if (sender != null && !senderName.equals(receiverName)) {
            try {
                sender.sendMessage(message); // 也发给发送者确认
                successCount++;
            } catch (Exception e) {
                logger.warn("向发送者回显私聊消息失败: " + e.getMessage());
            }
        }
        
        logger.info("私聊消息: " + senderName + " -> " + receiverName + ": " + message.getContent());
        
        return BroadcastResult.success(successCount);
    }
    
    /**
     * 广播用户列表更新
     * 
     * @return 广播结果
     */
    public BroadcastResult broadcastUserListUpdate() {
        Set<String> onlineUsers = userManager.getOnlineUsernames();
        String userListData = String.join(",", onlineUsers);
        
        Message updateMsg = new Message(Message.USER_LIST_UPDATE, "系统", userListData);
        
        BroadcastResult result = broadcastToAll(updateMsg);
        
        if (result.isSuccess()) {
            logger.info("广播用户列表更新: " + onlineUsers.size() + " 用户在线，" +
                       "发送给 " + result.getRecipientCount() + " 个客户端");
        }
        
        return result;
    }
    
    /**
     * 发送系统响应消息给指定用户
     * 
     * @param username 目标用户名
     * @param content 响应内容
     * @return 发送结果
     */
    public BroadcastResult sendSystemResponse(String username, String content) {
        if (username == null || username.trim().isEmpty()) {
            return BroadcastResult.failure("用户名不能为空");
        }
        
        if (content == null || content.trim().isEmpty()) {
            return BroadcastResult.failure("响应内容不能为空");
        }
        
        ClientHandler handler = userManager.getOnlineUserHandler(username);
        
        if (handler == null) {
            logger.warn("发送系统响应失败: 用户 " + username + " 不在线");
            return BroadcastResult.failure("用户不在线");
        }
        
        Message responseMsg = new Message(Message.SYSTEM_RESPONSE, "系统", content);
        
        try {
            handler.sendMessage(responseMsg);
            logger.debug("发送系统响应给 " + username + ": " + content);
            return BroadcastResult.success(1);
        } catch (Exception e) {
            logger.warn("发送系统响应失败: " + e.getMessage());
            return BroadcastResult.failure("发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 广播系统通知给所有在线用户
     * 
     * @param content 通知内容
     * @return 广播结果
     */
    public BroadcastResult broadcastSystemNotification(String content) {
        if (content == null || content.trim().isEmpty()) {
            return BroadcastResult.failure("通知内容不能为空");
        }
        
        Message notificationMsg = new Message(Message.SYSTEM_RESPONSE, "系统", content);
        
        BroadcastResult result = broadcastToAll(notificationMsg);
        
        if (result.isSuccess()) {
            logger.info("广播系统通知: " + content + 
                       " (发送给 " + result.getRecipientCount() + " 个用户)");
        }
        
        return result;
    }
    
    /**
     * 向指定用户列表发送消息
     * 
     * @param usernames 目标用户名列表
     * @param message 要发送的消息
     * @return 广播结果
     */
    public BroadcastResult sendToUsers(Collection<String> usernames, Message message) {
        if (usernames == null || usernames.isEmpty()) {
            return BroadcastResult.failure("目标用户列表为空");
        }
        
        if (message == null) {
            return BroadcastResult.failure("消息不能为空");
        }
        
        int successCount = 0;
        int targetCount = usernames.size();
        
        for (String username : usernames) {
            ClientHandler handler = userManager.getOnlineUserHandler(username);
            
            if (handler != null) {
                try {
                    handler.sendMessage(message);
                    successCount++;
                } catch (Exception e) {
                    logger.warn("向用户 " + username + " 发送消息失败: " + e.getMessage());
                }
            } else {
                logger.debug("用户 " + username + " 不在线，跳过发送");
            }
        }
        
        logger.info("向指定用户发送消息: " + message.getContent() + 
                   " (成功 " + successCount + "/" + targetCount + " 个用户)");
        
        return BroadcastResult.success(successCount);
    }
    
    /**
     * 获取广播统计信息
     * 
     * @return 统计信息字符串
     */
    public String getBroadcastStatistics() {
        int onlineCount = userManager.getOnlineUserCount();
        return String.format("消息广播统计 - 当前在线用户: %d", onlineCount);
    }
} 