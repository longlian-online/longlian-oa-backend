package online.longlian.app.service.admin;

import online.longlian.app.pojo.bo.AdminCreateParamsBO;
import online.longlian.app.service.admin.impl.AdminManagementServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 管理员引导测试，用于系统首次初始化时创建管理员账号。
 * 通过 {@link AdminManagementServiceImpl#createInternal} 无权限校验的纯创建方法，
 * 解除"需要 root 才能创建 root"的鸡生蛋依赖。
 */
@SpringBootTest
@ActiveProfiles
@Disabled
class AdminBootstrapTest {

    @Autowired
    private AdminManagementServiceImpl impl;

    /**
     * 创建 root 管理员并打印明文密码，方便配置后通过外部接口登录。
     * 加 @Disabled 防止随 test suite 自动执行。
     */
    @Test
    void createRootAdmin() {
        String username = "root";
        String password = "root123";

        Long adminId = impl.createInternal(
                AdminCreateParamsBO.builder()
                        .username(username)
                        .password(password)
                        .build(),
                "root"
        );

        System.out.println("========== Root管理员创建成功 ==========");
        System.out.println("ID: " + adminId);
        System.out.println("用户名: " + username);
        System.out.println("明文密码: " + password);
        System.out.println("角色: root");
        System.out.println("========================================");
    }
}
