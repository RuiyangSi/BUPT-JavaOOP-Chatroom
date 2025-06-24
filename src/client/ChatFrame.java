/*
 * ================================================================
 *  文件：ChatFrame.java   (客户端图形界面)
 * ---------------------------------------------------------------
 *  职责：
 *   • 通过 Swing 构建富文本聊天窗口；
 *   • 使用 JTextPane + StyledDocument 渲染多彩消息气泡；
 *   • 在右侧展示实时在线用户列表 (JList)；
 *   • 监听输入框 / 发送按钮 / 用户双击 等事件，并回调到 ChatClient；
 *
 *  视觉规范：
 *   - 自己发送的昵称/文本为绿色，其他用户蓝色，私聊橙色，@提及红色，系统灰色；
 *   - 信息气泡背景分左右对齐，仿微信风格；
 *
 *  交互亮点：
 *   1) 双击在线用户 -> 自动在输入框填充 "@" 用户名 " 便于私聊；
 *   2) 底部帮助栏列出所有系统命令；
 *   3) 状态栏指示是否匿名；
 *
 *  ♻️ 线程注意：所有来自网络线程的 UI 更新必须通过 invokeLater()。
 * ================================================================
 */
package client;


import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * 聊天界面 - 智能功能版本
 */
public class ChatFrame extends JFrame {
    private ChatClient client;
    private String username;
    private JTextPane chatPane;
    private JTextField inputField;
    private JButton sendButton;
    private JLabel statusLabel;
    private JLabel anonymousLabel;
    private JList<String> onlineUsersList;
    private JScrollPane usersScrollPane;
    private List<String> onlineUsers;
    private MessageStyleRenderer messageRenderer;
    
    // 颜色常量 - 保留用于用户列表等其他UI组件
    private static final Color MY_NAME_COLOR = new Color(34, 139, 34);
    private static final Color OTHER_NAME_COLOR = new Color(30, 144, 255);
    
    /**
     * 构造函数
     *
     * @param client   ChatClient 控制器
     * @param username 当前登录用户
     */
    public ChatFrame(ChatClient client, String username) {
        this.client = client;
        this.username = username;
        this.onlineUsers = new ArrayList<>();
        initializeComponents();
        this.messageRenderer = new MessageStyleRenderer(chatPane); // 初始化消息渲染器
        setupLayout();
        setupEventHandlers();
        setupFrame();
    }
    
    /**
     * 创建并风格化所有 Swing 组件（不做布局）
     */
    private void initializeComponents() {
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        chatPane.setBackground(new Color(248, 248, 248));
        
        inputField = new JTextField(40);
        inputField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        sendButton = new JButton("发送");
        sendButton.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        sendButton.setBackground(new Color(64, 158, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        
        statusLabel = new JLabel("已连接到服务器");
        statusLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(0, 128, 0));
        
        anonymousLabel = new JLabel("实名聊天");
        anonymousLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        anonymousLabel.setForeground(new Color(60, 120, 180));
        
        onlineUsersList = new JList<>();
        onlineUsersList.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        onlineUsersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineUsersList.setBorder(BorderFactory.createTitledBorder("👥 在线用户"));
        onlineUsersList.setBackground(new Color(248, 250, 252));
        
        onlineUsersList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                String user = value.toString();
                if (user.contains("(我)")) {
                    setIcon(createColorIcon(MY_NAME_COLOR));
                    setForeground(MY_NAME_COLOR);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setIcon(createColorIcon(OTHER_NAME_COLOR));
                    setForeground(OTHER_NAME_COLOR);
                }
                
                if (isSelected) {
                    setBackground(new Color(230, 240, 255));
                } else {
                    setBackground(index % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                
                return this;
            }
        });
        
        usersScrollPane = new JScrollPane(onlineUsersList);
        usersScrollPane.setPreferredSize(new Dimension(150, 0));
        usersScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLoweredBevelBorder()
        ));
        
        getRootPane().setDefaultButton(sendButton);
    }
    
    /**
     * 通过 BorderLayout/FlowLayout 等设置整体排版
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(240, 240, 240));
        
        JLabel userLabel = new JLabel("🙋‍♂️ 用户: " + username, SwingConstants.LEFT);
        userLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        userLabel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 10));
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setBackground(new Color(240, 240, 240));
        statusPanel.add(anonymousLabel);
        statusPanel.add(new JLabel(" | "));
        statusPanel.add(statusLabel);
        
        topPanel.add(userLabel, BorderLayout.WEST);
        topPanel.add(statusPanel, BorderLayout.EAST);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(200, 200, 200)));
        
        add(topPanel, BorderLayout.NORTH);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        JScrollPane chatScrollPane = new JScrollPane(chatPane);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("💬 聊天记录"));
        
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        mainPanel.add(usersScrollPane, BorderLayout.EAST);
        
        add(mainPanel, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(250, 250, 250));
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.setBackground(new Color(250, 250, 250));
        
        inputPanel.add(inputField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(new Color(250, 250, 250));
        buttonPanel.add(sendButton);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        JLabel helpLabel = new JLabel("<html><small>💡 <b>使用提示:</b> 直接输入广播 | @用户名 私聊 | <b>双击右侧用户</b> 快速私聊<br/>" +
                "🔧 <b>系统命令:</b> @@list (在线用户) | @@quit (退出) | @@anonymous (切换匿名) | @@showanonymous (查看匿名状态)</small></html>");
        helpLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        helpLabel.setForeground(new Color(100, 100, 100));
        helpLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 8, 15));
        helpLabel.setBackground(new Color(250, 250, 250));
        helpLabel.setOpaque(true);
        
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(helpLabel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 为按钮、键盘、列表等控件注册事件监听器
     */
    private void setupEventHandlers() {
        sendButton.addActionListener(e -> sendMessage());
        
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        
        onlineUsersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = onlineUsersList.getSelectedValue();
                    if (selectedUser != null) {
                        String pureUsername = extractPureUsername(selectedUser);
                        
                        // 检查是否点击自己
                        if (pureUsername != null && pureUsername.equals(username)) {
                            // 显示提示信息
                            JOptionPane.showMessageDialog(ChatFrame.this, 
                                "不能给自己发送私聊消息", "提示", 
                                JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        
                        if (pureUsername != null && !pureUsername.equals(username)) {
                            String currentText = inputField.getText();
                            if (currentText.startsWith("@")) {
                                int spaceIndex = currentText.indexOf(' ');
                                if (spaceIndex > 0) {
                                    inputField.setText("@" + pureUsername + currentText.substring(spaceIndex));
                                } else {
                                    inputField.setText("@" + pureUsername + " ");
                                }
                            } else {
                                inputField.setText("@" + pureUsername + " ");
                            }
                            inputField.requestFocus();
                            inputField.setCaretPosition(inputField.getText().length());
                        }
                    }
                }
            }
        });
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                String[] options = {"退出聊天室", "退出程序", "取消"};
                int option = JOptionPane.showOptionDialog(ChatFrame.this,
                    "要退出聊天室还是退出程序？",
                    "确认操作",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
                
                if (option == 0) { // 退出聊天室
                    client.logoutToLogin();
                } else if (option == 1) { // 退出程序
                    client.disconnect();
                }
                // option == 2 或其他值表示取消，不做任何操作
            }
        });
    }

    /**
     * 最终对 JFrame 做尺寸、标题、关闭操作等配置
     */
    private void setupFrame() {
        setTitle("💬 智能聊天室 - " + username);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 500));
        SwingUtilities.invokeLater(() -> inputField.requestFocus());
    }
    
    /**
     * 读取输入框内容并委托 ChatClient 发送
     */
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            client.sendChatMessage(message);
            inputField.setText("");
            inputField.requestFocus();
        }
    }
    
    /**
     * 在聊天面板显示一条消息
     *
     * @param sender    显示名称
     * @param content   文本内容
     * @param isPrivate 是否私聊
     */
    public void displayMessage(String sender, String content, boolean isPrivate) {
        SwingUtilities.invokeLater(() -> {
            // 判断是否是自己发送的消息
            boolean isFromMe = false;
            if (sender.startsWith("💬 " + username + "(我) ➤")) {
                // 自己发送的私聊消息
                isFromMe = true;
            } else if (sender.equals(username)) {
                // 自己发送的普通消息
                isFromMe = true;
            }
            
            // 使用MessageStyleRenderer渲染消息
            messageRenderer.renderMessage(sender, content, isPrivate, isFromMe, username);
        });
    }

    
    /**
     * 刷新右侧在线用户列表
     */
    public void updateOnlineUsers(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            this.onlineUsers.clear();
            this.onlineUsers.addAll(users);
            
            DefaultListModel<String> model = new DefaultListModel<>();
            for (String user : users) {
                String displayName;
                if (user.equals(username)) {
                    displayName = "🏠 " + user + " (我)";
                } else {
                    displayName = "👤 " + user;
                }
                model.addElement(displayName);
            }
            onlineUsersList.setModel(model);
            
            String title = String.format("👥 在线用户 (%d)", users.size());
            onlineUsersList.setBorder(BorderFactory.createTitledBorder(title));
        });
    }
    
    /**
     * 更新匿名状态标签
     */
    public void updateAnonymousStatus(boolean anonymous) {
        SwingUtilities.invokeLater(() -> {
            if (anonymous) {
                anonymousLabel.setText("🎭 匿名聊天");
                anonymousLabel.setForeground(new Color(255, 140, 0));
            } else {
                anonymousLabel.setText("👤 实名聊天");
                anonymousLabel.setForeground(new Color(60, 120, 180));
            }
        });
    }
    
    /**
     * 从 “用户名(我)” 文本中提取纯用户名
     */
    private String extractPureUsername(String displayName) {
        if (displayName == null) return null;
        
        String result = displayName;
        result = result.replace("🏠 ", "").replace("👤 ", "");
        
        if (result.endsWith(" (我)")) {
            result = result.substring(0, result.length() - 4);
        }
        
        return result.trim();
    }
    
    /**
     * 创建一个小色块图标，用于列表渲染
     */
    private Icon createColorIcon(Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(color);
                g.fillOval(x, y, getIconWidth(), getIconHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawOval(x, y, getIconWidth(), getIconHeight());
            }
            
            @Override
            public int getIconWidth() { return 8; }
            
            @Override
            public int getIconHeight() { return 8; }
        };
    }
    
    /**
     * 在状态栏更新连接状态
     */
    public void updateConnectionStatus(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                statusLabel.setText("🟢 已连接到服务器");
                statusLabel.setForeground(new Color(0, 128, 0));
            } else {
                statusLabel.setText("🔴 与服务器断开连接");
                statusLabel.setForeground(new Color(255, 0, 0));
            }
        });
    }
} 