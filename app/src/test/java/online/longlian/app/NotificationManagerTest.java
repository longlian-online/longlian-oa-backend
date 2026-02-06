package online.longlian.app;

import online.longlian.app.service.notify.NotificationManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 集成测试：加载 Spring 上下文，使用真实的邮件服务发送邮件
 * ActiveProfiles 指定使用 dev 环境配置（对应你的 spring.profiles.active=dev）
 */
@SpringBootTest // 关键：启动 Spring 上下文，加载所有配置和 Bean
@ActiveProfiles("dev") // 加载 dev 环境的配置文件（如 application-dev.yml）
class NotificationManagerTest {

    // 注入 Spring 容器中真实的 NotificationManager（包含真实的 EmailNotificationService）
    @Autowired
    private NotificationManager notificationManager;

    @Test
    void testSendRealEmail() {
        // 测试收件人（替换为你自己的QQ邮箱）
        String receiver = "test@qq.com";

        // 调用真实的发送逻辑
        notificationManager.send(receiver,"test:123456");

        System.out.println("邮件发送请求已提交，请检查收件箱/垃圾箱！");
    }
}