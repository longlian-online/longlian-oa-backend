package online.longlian.app.common.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

/**
邮件发送工具类
*/
@Component
public class MailUtil {
    private final JavaMailSender mailSender;

    public MailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public record SendParam(String sender, String receiver, String reveiverName, String title, String content) { }

    public void send(SendParam param) {
        Objects.requireNonNull(param, "SendParam cannot be null");
        if (!StringUtils.hasText(param.sender())) {
            throw new IllegalArgumentException("sender cannot be blank");
        }
        if (!StringUtils.hasText(param.receiver())) {
            throw new IllegalArgumentException("receiver cannot be blank");
        }
        if (!StringUtils.hasText(param.title())) {
            throw new IllegalArgumentException("title cannot be blank");
        }
        if (!StringUtils.hasText(param.content())) {
            throw new IllegalArgumentException("content cannot be blank");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 邮件名称
            if (param.reveiverName != null && !param.reveiverName.isEmpty()) {
                helper.setFrom(param.sender(), param.reveiverName());
            } else {
                helper.setFrom(param.sender());
            }
            helper.setTo(param.receiver());
            helper.setSubject(param.title());

            helper.setText(param.content(), true);
            mailSender.send(mimeMessage);
        } catch (UnsupportedEncodingException | MessagingException e) {
            throw new RuntimeException("发送邮件失败, 入参:" + param, e);
        }
    }
}
