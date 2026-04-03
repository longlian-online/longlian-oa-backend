package online.longlian.app;

import online.longlian.app.common.util.MailUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 集成测试：加载 Spring 上下文，使用真实的邮件服务发送测试邮件
 * ActiveProfiles 指定使用 dev 环境配置
 */
@SpringBootTest
@ActiveProfiles("dev")
class MailUtilTest {

    @Autowired
    private MailUtil mailUtil;

    @Value("${spring.mail.username:}")
    private String sender;

    @Value("${longlian.display-name:longlian-oa}")
    private String displayName;

    @Test
    void testSendEmail() {
        String receiver = "3474790137@qq.com";
        String title = "MailUtil";
        String content = "你好";

        mailUtil.send(new MailUtil.SendParam(sender, receiver, this.displayName, title, content));

        System.out.println("测试邮件已发送，请检查收件箱/垃圾箱！发件人: " + sender + ", 收件人: " + receiver);
    }
}
