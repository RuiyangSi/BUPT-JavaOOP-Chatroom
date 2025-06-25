/*
 * ================================================================
 *  文件：MessageStyleRenderer.java   (消息样式渲染器)
 * ---------------------------------------------------------------
 *  职责：
 *   • 负责将聊天消息以富文本形式渲染到 JTextPane；
 *   • 处理不同消息类型的样式：系统消息、私聊、普通聊天、@提及等；
 *   • 管理颜色、字体、图标等视觉元素；
 *   • 提供统一的消息格式化接口。
 *
 *  设计原则：
 *   - 单一职责：只负责消息的视觉渲染
 *   - 无状态：不保存任何UI状态
 *   - 可复用：可被不同的聊天界面使用
 * ================================================================
 */
package client;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 消息样式渲染器 - 负责聊天消息的富文本渲染
 */
public class MessageStyleRenderer {
    
    // 颜色常量 - 统一的配色方案
    private static final Color SYSTEM_MSG_COLOR = new Color(128, 128, 128);
    private static final Color PRIVATE_MSG_COLOR = new Color(255, 140, 0);
    private static final Color MENTION_COLOR = new Color(220, 20, 60);
    private static final Color NORMAL_MSG_COLOR = new Color(45, 45, 45);
    private static final Color MY_NAME_COLOR = new Color(34, 139, 34);
    private static final Color OTHER_NAME_COLOR = new Color(30, 144, 255);
    private static final Color TIME_COLOR = new Color(160, 160, 160);
    
    private final JTextPane textPane;
    
    /**
     * 构造函数
     * @param textPane 目标文本面板
     */
    public MessageStyleRenderer(JTextPane textPane) {
        this.textPane = textPane;
    }
    
    /**
     * 渲染消息到文本面板
     * 
     * @param sender 发送者名称
     * @param content 消息内容
     * @param isPrivate 是否为私聊消息
     * @param isFromMe 是否为自己发送的消息
     * @param currentUsername 当前用户名
     */
    public void renderMessage(String sender, String content, boolean isPrivate, boolean isFromMe, String currentUsername) {
        // 确定消息颜色
        Color msgColor = determineMessageColor(content, currentUsername);
        
        // 如果是@提及消息，播放提示音
        if (msgColor == MENTION_COLOR) {
            Toolkit.getDefaultToolkit().beep();
        }
        
        appendStyledMessage(sender, content, msgColor, isPrivate, isFromMe, currentUsername);
        
        // 自动滚动到底部
        textPane.setCaretPosition(textPane.getStyledDocument().getLength());
    }
    
    /**
     * 确定消息颜色
     */
    private Color determineMessageColor(String content, String currentUsername) {
        if (content.contains("@" + currentUsername)) {
            return MENTION_COLOR; // @提及消息用红色
        } else {
            return NORMAL_MSG_COLOR; // 普通消息用深灰色
        }
    }
    
    /**
     * 内部方法：将样式化消息追加到文档
     */
    private void appendStyledMessage(String sender, String content, Color color, boolean isPrivate, boolean isFromMe, String currentUsername) {
        StyledDocument doc = textPane.getStyledDocument();
        
        try {
            // 添加时间戳
            addTimestamp(doc);
            
            // 添加私聊标识
            if (isPrivate && !isSystemMessage(sender)) {
                addPrivateIndicator(doc);
            }
            
            // 添加发送者信息
            addSenderInfo(doc, sender, isFromMe, currentUsername);
            
            // 添加消息内容
            addMessageContent(doc, content, color);
            
            // 添加私聊分隔线
            if (isPrivate && !isSystemMessage(sender)) {
                addPrivateSeparator(doc);
            }
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 添加时间戳
     */
    private void addTimestamp(StyledDocument doc) throws BadLocationException {
        Style timeStyle = createTimeStyle();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        doc.insertString(doc.getLength(), "⏰ " + timestamp + " ", timeStyle);
    }
    
    /**
     * 添加私聊标识
     */
    private void addPrivateIndicator(StyledDocument doc) throws BadLocationException {
        Style privateStyle = createPrivateStyle();
        doc.insertString(doc.getLength(), "🔒 ", privateStyle);
    }
    
    /**
     * 添加发送者信息
     */
    private void addSenderInfo(StyledDocument doc, String sender, boolean isFromMe, String currentUsername) throws BadLocationException {
        if (sender.equals("系统")) {
            addSystemSender(doc);
        } else if (sender.startsWith("💬 " + currentUsername + "(我) ➤")) {
            addMyPrivateMessageSender(doc, sender, currentUsername);
        } else if (sender.startsWith("💌")) {
            addOtherPrivateMessageSender(doc, sender, isFromMe);
        } else {
            addNormalSender(doc, sender, isFromMe, currentUsername);
        }
    }
    
    /**
     * 添加系统发送者
     */
    private void addSystemSender(StyledDocument doc) throws BadLocationException {
        Style systemStyle = createSystemStyle();
        doc.insertString(doc.getLength(), "⚙️ 系统: ", systemStyle);
    }
    
    /**
     * 添加我发送的私聊消息发送者
     */
    private void addMyPrivateMessageSender(StyledDocument doc, String sender, String currentUsername) throws BadLocationException {
        // "💬 " 部分
        Style senderStyle = createSenderStyle(true);
        doc.insertString(doc.getLength(), "💬 ", senderStyle);
        
        // "username(我)" 用绿色
        Style myStyle = createMyNameStyle();
        doc.insertString(doc.getLength(), currentUsername + "(我)", myStyle);
        
        // " ➤ alice" 用蓝色
        Style targetStyle = createTargetNameStyle();
        String targetPart = sender.substring(("💬 " + currentUsername + "(我)").length());
        doc.insertString(doc.getLength(), targetPart + ": ", targetStyle);
    }
    
    /**
     * 添加别人发送的私聊消息发送者
     */
    private void addOtherPrivateMessageSender(StyledDocument doc, String sender, boolean isFromMe) throws BadLocationException {
        Style senderStyle = createSenderStyle(isFromMe);
        doc.insertString(doc.getLength(), sender + ": ", senderStyle);
    }
    
    /**
     * 添加普通消息发送者
     */
    private void addNormalSender(StyledDocument doc, String sender, boolean isFromMe, String currentUsername) throws BadLocationException {
        Style senderStyle = createSenderStyle(isFromMe);
        String displaySender = formatNormalSender(sender, currentUsername);
        doc.insertString(doc.getLength(), displaySender + ": ", senderStyle);
    }
    
    /**
     * 格式化普通发送者显示名称
     */
    private String formatNormalSender(String sender, String currentUsername) {
        if (sender.equals(currentUsername)) {
            return "👤 " + sender + " (我)";
        } else if (sender.contains("匿名")) {
            return sender; // 匿名用户已经有emoji
        } else {
            return "👤 " + sender;
        }
    }
    
    /**
     * 添加消息内容
     */
    private void addMessageContent(StyledDocument doc, String content, Color color) throws BadLocationException {
        Style contentStyle = createContentStyle(color);
        String displayContent = formatMessageContent(content);
        doc.insertString(doc.getLength(), displayContent + "\n", contentStyle);
    }
    
    /**
     * 格式化消息内容，添加合适的emoji
     */
    private String formatMessageContent(String content) {
        if (content.contains("登录成功") || content.contains("欢迎")) {
            return "🎉 " + content;
        } else if (content.contains("用户列表") || content.contains("在线用户")) {
            return "📋 " + content;
        } else if (content.contains("匿名")) {
            return "🎭 " + content;
        } else if (content.contains("退出") || content.contains("再见")) {
            return "👋 " + content;
        }
        return content;
    }
    
    /**
     * 添加私聊分隔线
     */
    private void addPrivateSeparator(StyledDocument doc) throws BadLocationException {
        Style separatorStyle = createSeparatorStyle();
        doc.insertString(doc.getLength(), "─────────────────────\n", separatorStyle);
    }
    
    /**
     * 判断是否为系统消息
     */
    private boolean isSystemMessage(String sender) {
        return sender.equals("系统") || sender.startsWith("💌") || sender.startsWith("💬");
    }
    
    // ==================== 样式创建方法 ====================
    
    private Style createTimeStyle() {
        Style timeStyle = textPane.addStyle("time", null);
        StyleConstants.setForeground(timeStyle, TIME_COLOR);
        StyleConstants.setFontSize(timeStyle, 11);
        StyleConstants.setItalic(timeStyle, true);
        return timeStyle;
    }
    
    private Style createPrivateStyle() {
        Style privateStyle = textPane.addStyle("private", null);
        StyleConstants.setForeground(privateStyle, PRIVATE_MSG_COLOR);
        StyleConstants.setBold(privateStyle, true);
        return privateStyle;
    }
    
    private Style createSenderStyle(boolean isFromMe) {
        Style senderStyle = textPane.addStyle("sender", null);
        StyleConstants.setForeground(senderStyle, isFromMe ? MY_NAME_COLOR : OTHER_NAME_COLOR);
        StyleConstants.setBold(senderStyle, true);
        StyleConstants.setFontSize(senderStyle, 13);
        return senderStyle;
    }
    
    private Style createSystemStyle() {
        Style systemStyle = textPane.addStyle("system", null);
        StyleConstants.setForeground(systemStyle, SYSTEM_MSG_COLOR);
        StyleConstants.setBold(systemStyle, true);
        return systemStyle;
    }
    
    private Style createMyNameStyle() {
        Style myStyle = textPane.addStyle("myName", null);
        StyleConstants.setForeground(myStyle, MY_NAME_COLOR);
        StyleConstants.setBold(myStyle, true);
        StyleConstants.setFontSize(myStyle, 13);
        return myStyle;
    }
    
    private Style createTargetNameStyle() {
        Style targetStyle = textPane.addStyle("targetName", null);
        StyleConstants.setForeground(targetStyle, OTHER_NAME_COLOR);
        StyleConstants.setBold(targetStyle, true);
        StyleConstants.setFontSize(targetStyle, 13);
        return targetStyle;
    }
    
    private Style createContentStyle(Color color) {
        Style contentStyle = textPane.addStyle("content", null);
        StyleConstants.setForeground(contentStyle, color);
        StyleConstants.setFontSize(contentStyle, 13);
        
        // 系统消息使用斜体
        if (color == SYSTEM_MSG_COLOR) {
            StyleConstants.setItalic(contentStyle, true);
        }
        
        return contentStyle;
    }
    
    private Style createSeparatorStyle() {
        Style separatorStyle = textPane.addStyle("separator", null);
        StyleConstants.setForeground(separatorStyle, new Color(230, 230, 230));
        StyleConstants.setFontSize(separatorStyle, 10);
        return separatorStyle;
    }
} 