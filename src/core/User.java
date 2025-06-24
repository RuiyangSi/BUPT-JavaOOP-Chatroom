/*
 * =============================================================
 *  文件：User.java  (公共模块)
 * -------------------------------------------------------------
 *  用于描述【用户实体】的简单 POJO。服务器启动时会从 users.txt
 *  加载所有用户信息，并在运行时更新 online / ipAddress 字段。
 *
 *  字段解释：
 *    • username   —— 登录用户名；唯一标识。
 *    • password   —— 登录口令；真实生产应使用哈希加盐，这里为简化存明文。
 *    • online     —— 是否在线；服务器在用户登录/退出时维护。
 *    • ipAddress  —— 最近一次登录的客户端 IP，用于审计或后台显示。
 *
 *  注意：类中未加入 Serializable，因为它只在服务器进程内部使用。
 * =============================================================
 */
package core;

/**
 * 用户类 - 存储用户基本信息
 */
public class User {
    private String username;
    private String password;
    private boolean online;
    private String ipAddress;
    
    /**
     * 无参构造函数 - 供框架或反射调用。
     */
    public User() {}
    
    /**
     * 带用户名/密码的构造函数
     *
     * @param username 用户名(唯一键)
     * @param password 密码(明文或加密串)
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.online = false;
    }
    
    /**
     * 获取用户名
     * @return username 登录名
     */
    public String getUsername() { return username; }
    
    /**
     * 设置用户名
     * @param username 登录名
     */
    public void setUsername(String username) { this.username = username; }
    
    /**
     * 获取密码
     * @return password 明文/加密
     */
    public String getPassword() { return password; }
    
    /**
     * 设置密码
     * @param password 密码串
     */
    public void setPassword(String password) { this.password = password; }
    
    /**
     * 判断是否在线
     * @return true 在线
     */
    public boolean isOnline() { return online; }
    
    /**
     * 设置在线状态
     * @param online true 表示在线
     */
    public void setOnline(boolean online) { this.online = online; }
    
    /**
     * 获取上次登录 IP
     * @return IP 地址
     */
    public String getIpAddress() { return ipAddress; }
    
    /**
     * 记录上次登录 IP
     * @param ipAddress 客户端 IP
     */
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    /**
     * 便于日志打印
     */
    @Override
    public String toString() {
        return String.format("User{username='%s', online=%s, ip='%s'}", username, online, ipAddress);
    }
    
    /**
     * 比较用户名是否相同
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return username != null ? username.equals(user.username) : user.username == null;
    }
    
    /**
     * 根据用户名生成 hashCode
     */
    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }
}