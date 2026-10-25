package online.longlian.app.service.notify;

import online.longlian.app.common.util.MailUtil;
import online.longlian.app.service.notify.impl.EmailNotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.util.HtmlUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailNotificationServiceUnitTest {

    @Test
    void shouldEscapeDisplayNameWithoutExpandingItsPlaceholderLikeText() {
        MailUtil mailUtil = mock(MailUtil.class);
        String displayName = "龙联 <script>alert(\"OA\")</script> & '组' ${code} ${receiver}";
        EmailNotificationService service = new EmailNotificationService(
                mailUtil,
                new DefaultResourceLoader(),
                "sender@example.com",
                displayName,
                "classpath:templates/verify-code.html");

        service.send("receiver@example.com", "123456");

        ArgumentCaptor<MailUtil.SendParam> captor = ArgumentCaptor.forClass(MailUtil.SendParam.class);
        verify(mailUtil).send(captor.capture());
        assertThat(captor.getValue().content())
                .contains(HtmlUtils.htmlEscape(displayName))
                .doesNotContain(displayName);
    }
}
