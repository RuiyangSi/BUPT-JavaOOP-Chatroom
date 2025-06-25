/*
 * ================================================================
 *  文件：MessageParser.java   (消息解析器)
 * ---------------------------------------------------------------
 *  职责：
 *   • 解析用户输入文本，识别消息类型（普通聊天、私聊、系统命令）；
 *   • 验证消息格式的正确性；
 *   • 创建相应的Message对象；
 *   • 提供统一的消息解析接口。
 *
 *  设计原则：
 *   - 单一职责：只负责消息解析和验证
 *   - 无状态：不保存任何会话状态
 *   - 可扩展：易于添加新的消息类型
 * ================================================================
 */
package messaging;

import core.Message;

/**
 * 消息解析器 - 负责解析和验证用户输入
 */
public class MessageParser {
    
    /**
     * 解析结果类 - 封装解析后的消息信息
     */
    public static class ParseResult {
        private final MessageType type;
        private final String content;
        private final String targetUser;
        private final String command;
        private final String errorMessage;
        private final boolean valid;
        
        // 私有构造函数，只能通过静态方法创建
        private ParseResult(MessageType type, String content, String targetUser, String command, String errorMessage, boolean valid) {
            this.type = type;
            this.content = content;
            this.targetUser = targetUser;
            this.command = command;
            this.errorMessage = errorMessage;
            this.valid = valid;
        }
        
        // 成功解析的工厂方法
        public static ParseResult success(MessageType type, String content, String targetUser, String command) {
            return new ParseResult(type, content, targetUser, command, null, true);
        }
        
        // 解析错误的工厂方法
        public static ParseResult error(String errorMessage) {
            return new ParseResult(null, null, null, null, errorMessage, false);
        }
        
        // Getter方法
        public MessageType getType() { return type; }
        public String getContent() { return content; }
        public String getTargetUser() { return targetUser; }
        public String getCommand() { return command; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isValid() { return valid; }
    }
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        NORMAL_CHAT,     // 普通聊天
        PRIVATE_CHAT,    // 私聊
        SYSTEM_COMMAND,  // 系统命令
        LOCAL_COMMAND    // 本地命令（不发送到服务器）
    }
    
    /**
     * 解析用户输入文本
     * 
     * @param input 用户输入的原始文本
     * @param currentUsername 当前用户名（用于验证）
     * @return 解析结果
     */
    public static ParseResult parseInput(String input, String currentUsername) {
        // 空输入检查
        if (input == null || input.trim().isEmpty()) {
            return ParseResult.error("消息内容不能为空");
        }
        
        String trimmed = input.trim();
        
        // 检查是否是系统命令
        if (trimmed.startsWith("@@")) {
            return parseSystemCommand(trimmed);
        }
        
        // 检查是否是私聊消息
        if (trimmed.startsWith("@") && !trimmed.startsWith("@@")) {
            return parsePrivateMessage(trimmed, currentUsername);
        }
        
        // 普通聊天消息
        return ParseResult.success(MessageType.NORMAL_CHAT, trimmed, null, null);
    }
    
    /**
     * 解析系统命令
     */
    private static ParseResult parseSystemCommand(String input) {
        String command = input.substring(2).trim(); // 去掉 "@@"
        
        if (command.isEmpty()) {
            return ParseResult.error("系统命令不能为空");
        }
        
        // 检查是否是本地命令（不需要发送到服务器）
        if (isLocalCommand(command)) {
            return ParseResult.success(MessageType.LOCAL_COMMAND, null, null, command);
        }
        
        // 验证系统命令是否有效
        if (isValidSystemCommand(command)) {
            return ParseResult.success(MessageType.SYSTEM_COMMAND, null, null, command);
        } else {
            return ParseResult.error("未知的系统命令: " + command);
        }
    }
    
    /**
     * 解析私聊消息
     */
    private static ParseResult parsePrivateMessage(String input, String currentUsername) {
        int spaceIndex = input.indexOf(' ');
        
        if (spaceIndex <= 1) {
            return ParseResult.error("私聊格式错误，请使用: @用户名 消息内容");
        }
        
        String targetUser = input.substring(1, spaceIndex).trim();
        String messageContent = input.substring(spaceIndex + 1).trim();
        
        // 验证目标用户名
        if (targetUser.isEmpty()) {
            return ParseResult.error("目标用户名不能为空");
        }
        
        // 验证消息内容
        if (messageContent.isEmpty()) {
            return ParseResult.error("私聊消息内容不能为空");
        }
        
        // 检查是否给自己发消息
        if (targetUser.equals(currentUsername)) {
            return ParseResult.error("❌ 不能给自己发送私聊消息");
        }
        
        // 验证用户名格式（简单验证：不包含特殊字符）
        if (!isValidUsername(targetUser)) {
            return ParseResult.error("用户名格式无效");
        }
        
        return ParseResult.success(MessageType.PRIVATE_CHAT, messageContent, targetUser, null);
    }
    
    /**
     * 检查是否是本地命令
     */
    private static boolean isLocalCommand(String command) {
        String lowerCommand = command.toLowerCase();
        return "showanonymous".equals(lowerCommand);
    }
    
    /**
     * 验证系统命令是否有效
     */
    private static boolean isValidSystemCommand(String command) {
        String lowerCommand = command.toLowerCase();
        return "list".equals(lowerCommand) || 
               "quit".equals(lowerCommand) || 
               "anonymous".equals(lowerCommand);
    }
    
    /**
     * 验证用户名格式
     */
    private static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // 简单验证：只允许字母、数字和下划线，长度3-20字符
        return username.matches("^[a-zA-Z0-9_]{1,20}$");
    }
    
    /**
     * 创建Message对象
     * 
     * @param parseResult 解析结果
     * @param username 发送者用户名
     * @param anonymous 是否匿名
     * @return Message对象，如果解析失败返回null
     */
    public static Message createMessage(ParseResult parseResult, String username, boolean anonymous) {
        if (!parseResult.isValid()) {
            return null;
        }
        
        Message message;
        
        switch (parseResult.getType()) {
            case NORMAL_CHAT:
                message = new Message(Message.CHAT, username, parseResult.getContent());
                break;
                
            case PRIVATE_CHAT:
                message = new Message(Message.PRIVATE_CHAT, username, parseResult.getTargetUser(), parseResult.getContent());
                break;
                
            case SYSTEM_COMMAND:
                message = new Message(Message.SYSTEM_COMMAND, username, parseResult.getCommand());
                break;
                
            default:
                return null;
        }
        
        message.setAnonymous(anonymous);
        return message;
    }
    
    /**
     * 获取支持的系统命令列表
     */
    public static String[] getSupportedSystemCommands() {
        return new String[]{"list", "quit", "anonymous", "showanonymous"};
    }
    
    /**
     * 获取系统命令帮助文本
     */
    public static String getSystemCommandHelp() {
        return "支持的系统命令：\n" +
               "@@list - 查看在线用户列表\n" +
               "@@quit - 退出聊天室\n" +
               "@@anonymous - 切换匿名模式\n" +
               "@@showanonymous - 查看当前匿名状态";
    }
    
    /**
     * 获取使用帮助文本
     */
    public static String getUsageHelp() {
        return "💡 使用提示：\n" +
               "• 直接输入文字 - 发送广播消息\n" +
               "• @用户名 消息内容 - 发送私聊消息\n" +
               "• @@命令 - 执行系统命令\n\n" +
               getSystemCommandHelp();
    }
} 