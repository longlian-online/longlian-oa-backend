package online.longlian.app.service.notify.impl;

import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.enumeration.NotificationType;
import online.longlian.app.common.util.MailUtil;
import online.longlian.app.service.notify.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailNotificationService implements NotificationService {

    private final ResourceLoader resourceLoader;
    private final MailUtil mailUtil;
    private final String fromEmail;
    private final String templatePath;
    private final String htmlTemplate;

    public EmailNotificationService(
            MailUtil mailUtil,
            ResourceLoader resourceLoader,
            @Value("${spring.mail.username}") String fromEmail,
            @Value("${notify.email.template.path}") String templatePath) {
        this.resourceLoader = resourceLoader;
        this.mailUtil = mailUtil;
        this.fromEmail = fromEmail;
        this.templatePath = templatePath;
        this.htmlTemplate = loadHtmlTemplate();
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }

    @Override
    public void send(String receiver, String code) {
        String htmlContent = htmlTemplate.replace("${code}", code).replace("${receiver}", receiver);
        mailUtil.send(fromEmail, receiver, CommonConstants.NOTIFY_TITLE, htmlContent);
    }

    private String loadHtmlTemplate() {
        try {
            // 2. 加载模板文件
            Resource resource = resourceLoader.getResource(templatePath);
            // 3. 校验文件存在
            if (!resource.exists()) {
                throw new IOException("邮件模板文件不存在：" + templatePath);
            }
            // 4. 读取模板内容
            byte[] contentBytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            return new String(contentBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("加载邮件HTML模板失败，路径：" + templatePath, e);
        }
    }
}