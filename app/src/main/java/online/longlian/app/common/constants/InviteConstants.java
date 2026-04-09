package online.longlian.app.common.constants;

public class InviteConstants {

    // 管理员邀请新用户注册后加入组织
    public static final String INVITE_MODE_ORG_ADMIN_JOIN = "ORG_ADMIN_JOIN";
    // 超管邀请新用户注册并创建组织
    public static final String INVITE_MODE_SUPER_ADMIN_CREATE_ORG = "SUPER_ADMIN_CREATE_ORG";

    public static final String ROLE_ORG_ADMIN = "ORG_ADMIN";

    public static final int INVITE_EXPIRE_MINUTES = 30;
    public static final int INVITE_TOKEN_LENGTH = 24;
    public static final int INVITE_CODE_LENGTH = 6;

    public static final String INVITE_URL_PATH = "/app/user/register/invite?inviteToken=";
    public static final String DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private InviteConstants() {
    }
}
