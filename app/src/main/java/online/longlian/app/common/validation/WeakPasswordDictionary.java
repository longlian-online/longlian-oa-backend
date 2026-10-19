package online.longlian.app.common.validation;

import online.longlian.app.common.constants.CommonConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WeakPasswordDictionary {

    private final Set<String> passwords;

    public WeakPasswordDictionary() {
        this(new ClassPathResource(CommonConstants.WEAK_PASSWORD_DICTIONARY_PATH));
    }

    WeakPasswordDictionary(Resource resource) {
        // 字典不可用时拒绝启动，避免部署后悄悄放行弱密码。
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            passwords = reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException | java.io.UncheckedIOException e) {
            throw new IllegalStateException("无法加载弱密码字典", e);
        }
        if (passwords.isEmpty()) {
            throw new IllegalStateException("弱密码字典不能为空");
        }
    }

    public boolean contains(String password) {
        return passwords.contains(password.toLowerCase(Locale.ROOT));
    }
}
