package online.longlian.app.service.notify.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.enumeration.NotificationType;
import online.longlian.app.service.notify.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender;
    private final ResourceLoader resourceLoader;
    @Value("${spring.mail.username}")
    private String fromEmail;
    @Value("${notify.email.template.path}")
    private String TEMPLATE_PATH;
    private final String htmlTemplate;
    public EmailNotificationService(JavaMailSender mailSender, ResourceLoader resourceLoader) {
        this.mailSender = mailSender;
        this.resourceLoader = resourceLoader;
        this.htmlTemplate = loadHtmlTemplate();
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }

    @Override
    public void send(String receiver, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);

            helper.setTo(receiver);
            helper.setSubject(CommonConstants.NOTIFY_TITLE);

            String htmlContent = htmlTemplate
                    .replace("${code}", code)
                    .replace("${receiver}", receiver);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("发送验证码邮件失败", e);
        }
    }

    private String loadHtmlTemplate() {
        try {
            Resource resource = resourceLoader.getResource(TEMPLATE_PATH);
            byte[] contentBytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            return new String(contentBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("加载邮件HTML模板失败", e);
        }
    }
}