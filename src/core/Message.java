/*
 * ===================================================================
 *  文件：Message.java  (公共模块)
 * -------------------------------------------------------------------
 *  聊天室项目中的【数据载体】—— 客户端与服务器之间所有信息均通过
 *  本类进行序列化后在网络上传输。
 *
 *  1) 设计目标
 *     • 使用 Serializable 以便通过 ObjectInput/OutputStream 直接传输；
 *     • 通过字符串常量定义【强类型】消息类别，避免硬编码；
 *     • 支持扩展：新增消息类型或字段时，对现有代码影响最小；
 *
 *  2) 字段说明
 *     • type      —— 消息类型(参见下方常量)，驱动业务分支处理。
 *     • sender    —— 发送方用户名(或系统)。
 *     • receiver  —— 接收方用户名，仅私聊/定向消息使用。
 *     • content   —— 具体文本内容(或命令内容)。
 *     • timestamp —— 发送时间，采用 ISO-8601 字符串，便于日志。
 *     • anonymous —— 标记发送者是否匿名(客户端在 UI 上用 🎭 标识)。
 *
 *  3) 常量一览
 *        LOGIN, LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT,
 *        CHAT, PRIVATE_CHAT,
 *        SYSTEM_COMMAND, SYSTEM_RESPONSE,
 *        USER_LIST, USER_LIST_UPDATE,
 *        ANONYMOUS_TOGGLE
 *
 *     通过这些常量可实现：登录流程、广播聊天、私聊、系统指令交互、
 *     在线用户广播、匿名切换等核心功能。
 *
 *  4) 典型使用场景
 *     • 客户端登录：new Message(LOGIN,  username,  "username:password")
 *     • 服务器应答：new Message(LOGIN_SUCCESS, "系统", "欢迎…")
 *     • 普通聊天：   new Message(CHAT,   username,  "大家好！")
 *     • 私聊：       new Message(PRIVATE_CHAT, from, to, "悄悄话…")
 *
 *  5) 维护提示
 *     • 若需添加新的字段，请同时更新 serialVersionUID 并补充 toString()
 *     • 若添加新的消息类型，请在客户端/服务器 switch 语句中补充处理逻辑。
 * ===================================================================
 */

package core;

import java.io.Serializable;

/**
 * 消息类 - 用于客户端和服务器之间的通信
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 消息类型常量
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String CHAT = "CHAT";
    public static final String PRIVATE_CHAT = "PRIVATE_CHAT";
    public static final String SYSTEM_COMMAND = "SYSTEM_COMMAND";
    public static final String SYSTEM_RESPONSE = "SYSTEM_RESPONSE";
    public static final String USER_LIST = "USER_LIST";
    public static final String USER_LIST_UPDATE = "USER_LIST_UPDATE";  // 新增：用户列表更新广播
    public static final String ANONYMOUS_TOGGLE = "ANONYMOUS_TOGGLE";
    
    private String type;        // 消息类型
    private String sender;      // 发送者
    private String receiver;    // 接收者（私聊时使用）
    private String content;     // 消息内容
    private String timestamp;   // 时间戳
    private boolean anonymous;  // 是否匿名
    
    /**
     * 无参构造函数
     * <p>主要供 Java 反序列化（ObjectInputStream.readObject）或
     *  框架通过反射创建对象时使用，业务代码通常不会直接调用。</p>
     */
    public Message() {}
    
    /**
     * 广播/系统消息构造函数
     *
     * @param type    消息类型常量，如 {@link #CHAT}、{@link #SYSTEM_COMMAND}
     * @param sender  发送方用户名，使用 "系统" 表示服务器端广播
     * @param content 纯文本内容，格式由调用方自定义
     */
    public Message(String type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
    
    /**
     * 私聊消息构造函数
     *
     * @param type    消息类型，应为 {@link #PRIVATE_CHAT}
     * @param sender  发起私聊的用户名
     * @param receiver 接收方用户名
     * @param content  私聊具体内容
     */
    public Message(String type, String sender, String receiver, String content) {
        this(type, sender, content);
        this.receiver = receiver;
    }
    
    /**
     * 获取消息类型
     * @return 消息类型常量字符串
     */
    public String getType() { return type; }
    
    /**
     * 设置消息类型
     * @param type 消息类型常量
     */
    public void setType(String type) { this.type = type; }
    
    /**
     * 获取发送者用户名
     * @return 发送者
     */
    public String getSender() { return sender; }
    
    /**
     * 设置发送者用户名
     * @param sender 发送者
     */
    public void setSender(String sender) { this.sender = sender; }
    
    /**
     * 获取接收者用户名（仅私聊使用）
     * @return 接收者
     */
    public String getReceiver() { return receiver; }
    
    /**
     * 设置接收者用户名（仅私聊使用）
     * @param receiver 接收者
     */
    public void setReceiver(String receiver) { this.receiver = receiver; }
    
    /**
     * 获取消息内容
     * @return 文本内容
     */
    public String getContent() { return content; }
    
    /**
     * 设置消息内容
     * @param content 文本内容
     */
    public void setContent(String content) { this.content = content; }
    
    /**
     * 获取时间戳（ISO-8601 格式）
     * @return 时间字符串
     */
    public String getTimestamp() { return timestamp; }
    
    /**
     * 手动设置时间戳（一般无需调用）
     * @param timestamp 时间字符串
     */
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    /**
     * 判断是否匿名
     * @return true = 匿名，false = 实名
     */
    public boolean isAnonymous() { return anonymous; }
    
    /**
     * 设置匿名标志
     * @param anonymous true 表示匿名
     */
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
    
    /**
     * 便于日志打印的字符串表示
     */
    @Override
    public String toString() {
        return String.format("Message{type='%s', sender='%s', receiver='%s', content='%s', anonymous=%s}", 
                           type, sender, receiver, content, anonymous);
    }
} 