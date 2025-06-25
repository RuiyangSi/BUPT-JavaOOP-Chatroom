/*
 * ======================================================
 *  文件：LoginFrame.java  (客户端登录界面)
 * ------------------------------------------------------
 *  仅负责收集用户名/密码并调用 ChatClient.attemptLogin()
 *  无任何业务逻辑，以保证 MVC 分层清晰。
 *
 *  界面特点：
 *    • GridBagLayout 进行表单排版；
 *    • 默认按钮设置为 loginButton，回车即可登录；
 *    • 底部展示预设用户名示例，方便测试。
 * ======================================================
 */
package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 登录界面
 */
public class LoginFrame extends JFrame {
    private ChatClient client;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField serverHostField;
    private JTextField serverPortField;
    private JButton loginButton;
    private JButton exitButton;
    
    /**
     * 构造函数
     *
     * @param client ChatClient 控制器
     */
    public LoginFrame(ChatClient client) {
        this.client = client;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupFrame();
    }
    
    /**
     * 创建所有表单控件
     */
    private void initializeComponents() {
        // 创建输入框
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        serverHostField = new JTextField("127.0.0.1", 15);
        serverPortField = new JTextField("8080", 8);
        
        // 创建按钮
        loginButton = new JButton("登录");
        exitButton = new JButton("退出");
        
        // 设置默认按钮
        getRootPane().setDefaultButton(loginButton);
    }
    
    /**
     * 使用 GridBagLayout 构建登录表单
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 标题
        JLabel titleLabel = new JLabel("聊天室登录", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 24));
        titleLabel.setForeground(new Color(60, 120, 180));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 30, 20);
        mainPanel.add(titleLabel, gbc);
        
        // 用户名标签和输入框
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.insets = new Insets(10, 20, 10, 10);
        mainPanel.add(new JLabel("用户名:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        mainPanel.add(usernameField, gbc);
        
        // 密码标签和输入框
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 20, 10, 10);
        mainPanel.add(new JLabel("密码:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        mainPanel.add(passwordField, gbc);
        
        // 服务器地址标签和输入框
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 20, 10, 10);
        mainPanel.add(new JLabel("服务器:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        mainPanel.add(serverHostField, gbc);
        
        // 端口标签和输入框
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 20, 10, 10);
        mainPanel.add(new JLabel("端口:"), gbc);
        
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 10, 10, 20);
        mainPanel.add(serverPortField, gbc);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(buttonPanel, gbc);
        
        // 添加提示信息
        JLabel hintLabel = new JLabel("<html><center>默认用户: admin/123456, alice/password, bob/123<br/>更多用户请查看data/users.txt文件</center></html>");
        hintLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        hintLabel.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 20, 20, 20);
        mainPanel.add(hintLabel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * 注册按钮、键盘事件
     */
    private void setupEventHandlers() {
        // 登录按钮事件
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                attemptLogin();
            }
        });
        
        // 退出按钮事件
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        
        // 回车键登录
        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptLogin();
                }
            }
        };
        
        usernameField.addKeyListener(enterKeyListener);
        passwordField.addKeyListener(enterKeyListener);
        serverHostField.addKeyListener(enterKeyListener);
        serverPortField.addKeyListener(enterKeyListener);
    }
    
    /**
     * 设置窗口属性（标题、大小、居中等）
     */
    private void setupFrame() {
        setTitle("聊天室 - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null); // 居中显示
        
        // 设置窗口图标（如果需要）
        try {
            // 这里可以设置窗口图标
            // setIconImage(ImageIO.read(new File("icon.png")));
        } catch (Exception e) {
            // 忽略图标加载错误
        }
        
        // 设置默认焦点
        SwingUtilities.invokeLater(() -> usernameField.requestFocus());
    }
    
    /**
     * 校验输入并调用 ChatClient.attemptLogin()
     */
    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String serverHost = serverHostField.getText().trim();
        String serverPortStr = serverPortField.getText().trim();
        
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名", "输入错误", JOptionPane.WARNING_MESSAGE);
            usernameField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入密码", "输入错误", JOptionPane.WARNING_MESSAGE);
            passwordField.requestFocus();
            return;
        }
        
        if (serverHost.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入服务器地址", "输入错误", JOptionPane.WARNING_MESSAGE);
            serverHostField.requestFocus();
            return;
        }
        
        int serverPort;
        try {
            serverPort = Integer.parseInt(serverPortStr);
            if (serverPort < 1 || serverPort > 65535) {
                throw new NumberFormatException("端口号超出范围");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的端口号(1-65535)", "输入错误", JOptionPane.WARNING_MESSAGE);
            serverPortField.requestFocus();
            return;
        }
        
        // 禁用登录按钮，防止重复点击
        loginButton.setEnabled(false);
        loginButton.setText("连接中...");
        
        // 调用客户端登录方法
        client.setUsername(username);
        client.attemptLogin(username, password, serverHost, serverPort);
        
        // 重新启用登录按钮（登录结果会在回调中处理）
        SwingUtilities.invokeLater(() -> {
            loginButton.setEnabled(true);
            loginButton.setText("登录");
        });
    }
} 