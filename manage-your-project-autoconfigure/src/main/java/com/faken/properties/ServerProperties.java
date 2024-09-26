package com.faken.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * 配置发送数据的目标服务器信息
 */
@Data
@ConfigurationProperties(prefix = "manage-platform")
public class ServerProperties {

    private String serverPort;
    private String serverIp;
    private String projectId;
    private boolean enabledAccessLog = false;
    private boolean enableErrorLog = false;
    private int initialDelay = 0;
    private int period = 5;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    // 使用代码块进行加载
//    {
//        Properties properties = new Properties();
//
//        // 获取当前类的类加载器, 读取properties文件, 对静态成员字段进行赋值
//        ClassLoader classLoader = ServerProperties.class.getClassLoader();
//
//        // 定死配置文件的命名为 settings.properties
//        URL resource = classLoader.getResource("settings.properties");
//        if (resource != null) {
//            try {
//                properties.load(new InputStreamReader(resource.openStream()));
//            } catch (IOException e) {
//                // TODO 自定义异常处理
//                throw new RuntimeException("Failed to load settings.properties", e);
//            }
//        }
//        try {
//            serverPort = properties.getProperty("server.port");
//            serverIP = properties.getProperty("server.ip");
//            projectId = properties.getProperty("project.id");
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to load settings.properties, maybe wrong key-value settings", e);
//        }
//    }

}
