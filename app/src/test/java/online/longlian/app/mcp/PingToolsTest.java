package online.longlian.app.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PingToolsTest {

    // 工具不依赖登录上下文、组织或业务服务，直接调用也应返回固定探活结果。
    @Test
    void shouldReturnPong() {
        assertThat(new PingTools().ping()).isEqualTo("pong");
    }
}
