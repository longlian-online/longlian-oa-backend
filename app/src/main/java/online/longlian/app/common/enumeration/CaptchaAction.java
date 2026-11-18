package online.longlian.app.common.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaptchaAction {
    APP_LOGIN_PWD("app_login_pwd"),
    APP_LOGIN_EMAIL("app_login_email"),
    ADMIN_LOGIN("admin_login");

    private final String value;
}
