package online.longlian.app.common.util;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailUtilUnitTest {

    @Test
    void shouldNotExposeEmailContentInFailureMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        MailUtil mailUtil = new MailUtil(mailSender);
        String code = "123456";
        String content = "<p>verification code: " + code + "</p>";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> mailUtil.send(
                new MailUtil.SendParam("sender@example.com", "invalid@", "Longlian", "Verification", content)
        ));

        assertFalse(exception.getMessage().contains(code));
        assertFalse(exception.getMessage().contains(content));
    }
}
