/*
 * ================================================================
 *  文件：UserManager.java   (用户管理器)
 * ---------------------------------------------------------------
 *  职责：
 *   • 管理用户数据的加载和保存；
 *   • 处理用户认证逻辑；
 *   • 维护在线用户状态；
 *   • 提供用户查询和管理接口。
 *
 *  设计原则：
 *   - 单一职责：只负责用户相关操作
 *   - 数据持久化：支持文件存储
 *   - 线程安全：支持并发访问
 * ================================================================
 */
package server;

import core.User;
import core.Logger;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器 - 负责服务器端的用户管理
 */
public class UserManager {
    
    private static final String DEFAULT_USER_FILE = "data/users.txt";
    
    private final Map<String, User> allUsers;           // 所有注册用户
    private final Map<String, ClientHandler> onlineClients;  // 在线用户及其处理器
    private final String userFilePath;
    private final Logger logger;
    
    /**
     * 用户状态监听器接口
     */
    public interface UserStatusListener {
        /**
         * 用户上线事件
         * @param username 用户名
         */
        void onUserOnline(String username);
        
        /**
         * 用户下线事件
         * @param username 用户名
         */
        void onUserOffline(String username);
    }
    
    private UserStatusListener statusListener;
    
    /**
     * 构造函数（使用默认用户文件）
     */
    public UserManager() {
        this(DEFAULT_USER_FILE);
    }
    
    /**
     * 构造函数
     * @param userFilePath 用户数据文件路径
     */
    public UserManager(String userFilePath) {
        this.userFilePath = userFilePath;
        this.allUsers = new ConcurrentHashMap<>();
        this.onlineClients = new ConcurrentHashMap<>();
        this.logger = Logger.getInstance();
        
        loadUsers();
    }
    
    /**
     * 设置用户状态监听器
     */
    public void setUserStatusListener(UserStatusListener listener) {
        this.statusListener = listener;
    }
    
    /**
     * 从文件加载用户数据
     */
    private void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(userFilePath))) {
            String line;
            int loadedCount = 0;
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String username = parts[0].trim();
                    String password = parts[1].trim();
                    
                    if (!username.isEmpty() && !password.isEmpty()) {
                        allUsers.put(username, new User(username, password));
                        loadedCount++;
                    }
                }
            }
            
            logger.info("成功加载 " + loadedCount + " 个用户账户");
            
        } catch (IOException e) {
            logger.warn("无法读取用户文件: " + e.getMessage() + "，将创建默认用户");
            createDefaultUsers();
        }
    }
    
    /**
     * 创建默认用户数据
     */
    private void createDefaultUsers() {
        String[] defaultUsers = {
            "admin:123456", "alice:password", "bob:123", "charlie:abc",
            "diana:pass", "eve:secret", "frank:test", "grace:hello",
            "henry:world", "iris:java", "jack:swing", "kate:chat"
        };
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(userFilePath))) {
            for (String userInfo : defaultUsers) {
                String[] parts = userInfo.split(":");
                if (parts.length == 2) {
                    String username = parts[0];
                    String password = parts[1];
                    allUsers.put(username, new User(username, password));
                    writer.println(userInfo);
                }
            }
            logger.info("创建了默认用户文件，包含 " + defaultUsers.length + " 个用户");
            
        } catch (IOException e) {
            logger.error("无法创建用户文件: " + e.getMessage());
        }
    }
    
    /**
     * 用户认证
     * 
     * @param username 用户名
     * @param password 密码
     * @param clientIP 客户端IP地址
     * @return true 表示认证成功
     */
    public boolean authenticateUser(String username, String password, String clientIP) {
        User user = allUsers.get(username);
        
        if (user != null && user.getPassword().equals(password)) {
            // 认证成功
            user.setOnline(true);
            user.setIpAddress(clientIP);
            logger.info("用户登录成功: " + username + " (IP: " + clientIP + ")");
            return true;
        } else {
            // 认证失败
            logger.warn("用户登录失败: " + username + " (IP: " + clientIP + ")");
            return false;
        }
    }
    
    /**
     * 添加在线用户
     * 
     * @param username 用户名
     * @param handler 客户端处理器
     * @return true 表示添加成功
     */
    public synchronized boolean addOnlineUser(String username, ClientHandler handler) {
        if (username == null || handler == null) {
            return false;
        }
        
        // 检查用户是否已经在线
        if (onlineClients.containsKey(username)) {
            logger.warn("用户 " + username + " 已经在线，拒绝重复登录");
            return false;
        }
        
        onlineClients.put(username, handler);
        
        // 更新用户状态
        User user = allUsers.get(username);
        if (user != null) {
            user.setOnline(true);
        }
        
        logger.info("用户上线: " + username + " (当前在线: " + onlineClients.size() + " 人)");
        
        // 通知监听器
        if (statusListener != null) {
            statusListener.onUserOnline(username);
        }
        
        return true;
    }
    
    /**
     * 移除在线用户
     * 
     * @param username 用户名
     */
    public synchronized void removeOnlineUser(String username) {
        if (username == null) {
            return;
        }
        
        ClientHandler removed = onlineClients.remove(username);
        
        if (removed != null) {
            // 更新用户状态
            User user = allUsers.get(username);
            if (user != null) {
                user.setOnline(false);
                logger.info("用户下线: " + username + " (IP: " + user.getIpAddress() + ")");
            }
            
            logger.info("用户离线: " + username + " (当前在线: " + onlineClients.size() + " 人)");
            
            // 通知监听器
            if (statusListener != null) {
                statusListener.onUserOffline(username);
            }
        }
    }
    
    /**
     * 获取在线用户列表
     * 
     * @return 在线用户名集合
     */
    public Set<String> getOnlineUsernames() {
        return new HashSet<>(onlineClients.keySet());
    }
    
    /**
     * 获取在线用户的客户端处理器
     * 
     * @param username 用户名
     * @return 客户端处理器，如果用户不在线返回null
     */
    public ClientHandler getOnlineUserHandler(String username) {
        return onlineClients.get(username);
    }
    
    /**
     * 获取所有在线用户的处理器
     * 
     * @return 在线用户处理器集合
     */
    public Collection<ClientHandler> getAllOnlineHandlers() {
        return new ArrayList<>(onlineClients.values());
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param username 用户名
     * @return true 表示用户在线
     */
    public boolean isUserOnline(String username) {
        return onlineClients.containsKey(username);
    }
    
    /**
     * 获取在线用户数量
     * 
     * @return 在线用户数量
     */
    public int getOnlineUserCount() {
        return onlineClients.size();
    }
    
    /**
     * 获取总用户数量
     * 
     * @return 总用户数量
     */
    public int getTotalUserCount() {
        return allUsers.size();
    }
    
    /**
     * 获取用户信息
     * 
     * @param username 用户名
     * @return 用户对象，如果用户不存在返回null
     */
    public User getUserInfo(String username) {
        return allUsers.get(username);
    }
    
    /**
     * 获取所有用户信息
     * 
     * @return 用户信息集合
     */
    public Collection<User> getAllUsers() {
        return new ArrayList<>(allUsers.values());
    }
    
    /**
     * 添加新用户（注册功能）
     * 
     * @param username 用户名
     * @param password 密码
     * @return true 表示添加成功
     */
    public synchronized boolean addUser(String username, String password) {
        if (username == null || password == null || 
            username.trim().isEmpty() || password.trim().isEmpty()) {
            return false;
        }
        
        // 检查用户是否已存在
        if (allUsers.containsKey(username)) {
            return false;
        }
        
        // 添加用户
        User newUser = new User(username, password);
        allUsers.put(username, newUser);
        
        // 保存到文件
        saveUsersToFile();
        
        logger.info("新用户注册: " + username);
        return true;
    }
    
    /**
     * 保存用户数据到文件
     */
    private void saveUsersToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(userFilePath))) {
            for (User user : allUsers.values()) {
                writer.println(user.getUsername() + ":" + user.getPassword());
            }
            logger.debug("用户数据已保存到文件");
            
        } catch (IOException e) {
            logger.error("保存用户数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 构造在线用户列表文本
     * 
     * @return 格式化的在线用户列表
     */
    public String getOnlineUsersListText() {
        StringBuilder sb = new StringBuilder("在线用户列表：\n");
        Set<String> onlineUsers = getOnlineUsernames();
        
        if (onlineUsers.isEmpty()) {
            sb.append("当前没有用户在线\n");
        } else {
            for (String username : onlineUsers) {
                sb.append("- ").append(username).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 获取用户列表摘要
     * 
     * @return 用户统计信息
     */
    public String getUserStatistics() {
        return String.format("用户统计 - 总用户: %d, 在线用户: %d", 
                           getTotalUserCount(), getOnlineUserCount());
    }
    
    /**
     * 关闭用户管理器，清理资源
     */
    public void shutdown() {
        logger.info("用户管理器关闭中...");
        
        // 保存用户数据
        saveUsersToFile();
        
        // 清理在线用户
        onlineClients.clear();
        
        logger.info("用户管理器已关闭");
    }
} 