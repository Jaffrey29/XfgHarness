package cn.walioffice.deepseek.harness.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 启动 deepseek-harness 的 Java 版工程。
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
@SpringBootApplication(scanBasePackages = "cn.walioffice.deepseek.harness")
@MapperScan("cn.walioffice.deepseek.harness.infrastructure.dao")
public class DeepseekHarnessJavaApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DeepseekHarnessJavaApplication.class, args);
    }
}
