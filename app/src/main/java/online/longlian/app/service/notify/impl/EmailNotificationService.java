package online.longlian.app.service.notify.impl;

import online.longlian.app.common.enumeration.NotificationType;
import online.longlian.app.pojo.dto.NotificationReqDTO;
import online.longlian.app.service.notify.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService implements NotificationService {


    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }

    @Override
    public void send(NotificationReqDTO request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(request.getReceiver());
        message.setSubject(request.getTitle());
        message.setText(request.getContent());
        mailSender.send(message);
    }
}