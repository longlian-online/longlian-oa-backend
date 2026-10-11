package online.longlian.app.api.util;

import online.longlian.app.api.BaseApiTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/** 复用 BaseApiTest 的数据库清理逻辑，同时不修改共享测试基类。 */
public class TokenRevocationCacheTestListener extends AbstractTestExecutionListener {
    @Override
    public void beforeTestMethod(TestContext testContext) {
        if (!BaseApiTest.class.isAssignableFrom(testContext.getTestClass())) {
            return;
        }
        StringRedisTemplate redis = testContext.getApplicationContext().getBean(StringRedisTemplate.class);
        var keys = redis.keys("auth:revocations:v2:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
