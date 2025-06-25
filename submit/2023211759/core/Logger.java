/*
 * =================================================================
 *  文件：Logger.java  (公共模块)
 * -----------------------------------------------------------------
 *  独立的日志管理类，支持多级别日志、多输出目标、异步写入等功能。
 *  设计目标：
 *    • 解耦业务逻辑与日志功能
 *    • 支持多种日志级别和格式
 *    • 提供线程安全的日志写入
 *    • 支持配置化管理
 * =================================================================
 */
package core;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 聊天室日志管理器
 * 
 * 功能特性：
 * - 支持多级别日志：DEBUG, INFO, WARN, ERROR
 * - 支持多输出：控制台、文件、自定义处理器
 * - 线程安全的异步写入
 * - 日志格式化和过滤
 */
public class Logger {
    
    // 日志级别枚举
    public enum Level {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR");
        
        private final int priority;
        private final String name;
        
        Level(int priority, String name) {
            this.priority = priority;
            this.name = name;
        }
        
        public int getPriority() { return priority; }
        public String getName() { return name; }
    }
    
    // 日志条目类
    public static class LogEntry {
        private final Level level;
        private final String message;
        private final String timestamp;
        private final String threadName;
        
        public LogEntry(Level level, String message) {
            this.level = level;
            this.message = message;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            this.threadName = Thread.currentThread().getName();
        }
        
        public Level getLevel() { return level; }
        public String getMessage() { return message; }
        public String getTimestamp() { return timestamp; }
        public String getThreadName() { return threadName; }
        
        public String format() {
            return String.format("[%s] [%s] [%s] %s", 
                timestamp, level.getName(), threadName, message);
        }
    }
    
    // 单例实例
    private static volatile Logger instance;
    
    // 配置参数
    private Level minLevel = Level.INFO;
    private String logFile = "server.log";
    private boolean enableConsole = true;
    private boolean enableFile = true;
    private boolean immediateWrite = true;  // 新增：立即写入模式
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 异步日志队列和写入线程
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    private PrintWriter fileWriter;
    private volatile boolean running = true;
    private Thread logWriterThread;
    
    /**
     * 私有构造函数（单例模式）
     */
    private Logger() {
        initializeLogger();
        startLogWriterThread();
    }
    
    /**
     * 获取Logger单例实例
     */
    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }
    
    /**
     * 配置日志器
     */
    public Logger config(Level minLevel, String logFile, boolean enableConsole, boolean enableFile) {
        return config(minLevel, logFile, enableConsole, enableFile, true);
    }
    
    /**
     * 配置日志器（完整版本）
     */
    public Logger config(Level minLevel, String logFile, boolean enableConsole, boolean enableFile, boolean immediateWrite) {
        this.minLevel = minLevel;
        this.logFile = logFile;
        this.enableConsole = enableConsole;
        this.enableFile = enableFile;
        this.immediateWrite = immediateWrite;
        
        // 重新初始化文件写入器
        if (enableFile) {
            initializeFileWriter();
        }
        
        return this;
    }
    
    /**
     * 初始化日志器
     */
    private void initializeLogger() {
        if (enableFile) {
            initializeFileWriter();
        }
    }
    
    /**
     * 初始化文件写入器
     */
    private void initializeFileWriter() {
        try {
            if (fileWriter != null) {
                fileWriter.close();
            }
            fileWriter = new PrintWriter(new FileWriter(logFile, true));
        } catch (IOException e) {
            System.err.println("无法初始化日志文件: " + e.getMessage());
            enableFile = false;
        }
    }
    
    /**
     * 启动异步日志写入线程
     */
    private void startLogWriterThread() {
        logWriterThread = new Thread(() -> {
            while (running || !logQueue.isEmpty()) {
                try {
                    LogEntry entry = logQueue.take();
                    writeLogEntry(entry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("日志写入异常: " + e.getMessage());
                }
            }
        }, "LogWriter");
        logWriterThread.setDaemon(false);  // 改为非daemon线程，确保日志写入完成
        logWriterThread.start();
    }
    
    /**
     * 写入日志条目到各种输出
     */
    private synchronized void writeLogEntry(LogEntry entry) {
        String formattedLog = entry.format();
        
        // 控制台输出
        if (enableConsole) {
            if (entry.getLevel() == Level.ERROR || entry.getLevel() == Level.WARN) {
                System.err.println(formattedLog);
                System.err.flush();  // 强制刷新错误输出
            } else {
                System.out.println(formattedLog);
                System.out.flush();  // 强制刷新标准输出
            }
        }
        
        // 文件输出
        if (enableFile && fileWriter != null) {
            fileWriter.println(formattedLog);
            fileWriter.flush();  // 立即刷新到文件
        }
    }
    
    /**
     * 记录日志的核心方法
     */
    private void log(Level level, String message) {
        if (level.getPriority() >= minLevel.getPriority()) {
            LogEntry entry = new LogEntry(level, message);
            
            if (immediateWrite) {
                // 立即写入模式：直接写入，不使用队列
                writeLogEntry(entry);
            } else {
                // 异步模式：放入队列
                logQueue.offer(entry);
            }
        }
    }
    
    // 公共日志方法
    public void debug(String message) { log(Level.DEBUG, message); }
    public void info(String message) { log(Level.INFO, message); }
    public void warn(String message) { log(Level.WARN, message); }
    public void error(String message) { log(Level.ERROR, message); }
    
    // 支持格式化的日志方法
    public void debug(String format, Object... args) { debug(String.format(format, args)); }
    public void info(String format, Object... args) { info(String.format(format, args)); }
    public void warn(String format, Object... args) { warn(String.format(format, args)); }
    public void error(String format, Object... args) { error(String.format(format, args)); }
    
    // 服务器专用的简化方法（保持向后兼容）
    public void serverLog(String message) {
        info("[SERVER] " + message);
    }
    
    /**
     * 关闭日志器
     */
    public void shutdown() {
        running = false;
        
        // 等待队列清空
        while (!logQueue.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 关闭文件写入器
        if (fileWriter != null) {
            fileWriter.close();
        }
        
        // 中断写入线程
        if (logWriterThread != null) {
            logWriterThread.interrupt();
        }
    }
    
    // Getter方法
    public Level getMinLevel() { return minLevel; }
    public boolean isConsoleEnabled() { return enableConsole; }
    public boolean isFileEnabled() { return enableFile; }
    public String getLogFile() { return logFile; }
} 