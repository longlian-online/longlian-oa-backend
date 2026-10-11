package online.longlian.app.common.handler;

import com.alibaba.fastjson2.JSON;
import online.longlian.app.common.annotation.NotWrap;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ResultResponseBodyAdviceTest {

    private final ResultResponseBodyAdvice advice = new ResultResponseBodyAdvice();

    @Test
    void shouldWrapValueWithAnnotatedMessage() throws Exception {
        MethodParameter parameter = methodParameter("message");

        Object result = advice.beforeBodyWrite("完成", parameter, MediaType.APPLICATION_JSON,
                null, null, null);

        assertThat(result).isInstanceOf(Result.class);
        assertThat(((Result<?>) result).getCode()).isEqualTo(ResultCode.SUCCESS.getCode());
        assertThat(((Result<?>) result).getMsg()).isEqualTo("处理完成");
        assertThat(((Result<?>) result).getData()).isEqualTo("完成");
    }

    @Test
    void shouldWrapNullWithoutData() throws Exception {
        Object result = advice.beforeBodyWrite(null, methodParameter("empty"), MediaType.APPLICATION_JSON,
                null, null, null);

        assertThat(result).isInstanceOf(Result.class);
        assertThat(((Result<?>) result).getMsg()).isEqualTo(ResultCode.SUCCESS.getMsg());
        assertThat(((Result<?>) result).getData()).isNull();
    }

    @Test
    void shouldKeepExistingResult() throws Exception {
        Result<Void> existing = Result.success("已完成");

        Object result = advice.beforeBodyWrite(existing, methodParameter("empty"), MediaType.APPLICATION_JSON,
                null, null, null);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void shouldSkipNotWrapEndpoint() throws Exception {
        assertThat(advice.supports(methodParameter("raw"), null)).isFalse();
    }

    @Test
    void shouldSupportNormalEndpoint() throws Exception {
        assertThat(advice.supports(methodParameter("empty"), null)).isTrue();
    }

    @Test
    void shouldKeepFastjsonIdAndEnumContracts() {
        String json = JSON.toJSONString(new SerializationFixture());

        assertThat(json).contains("\"id\":\"123\"");
        assertThat(JSON.toJSONString(ProjectStatus.IN_PROGRESS)).isEqualTo("\"进行中\"");
    }

    private MethodParameter methodParameter(String methodName) throws NoSuchMethodException {
        Method method = FixtureController.class.getDeclaredMethod(methodName);
        return new MethodParameter(method, -1);
    }

    private static class FixtureController {
        @ResponseMessage("处理完成")
        private String message() {
            return "完成";
        }

        private void empty() {
        }

        @NotWrap
        private String raw() {
            return "raw";
        }
    }

    private static class SerializationFixture {
        @JsonLongIdString
        private final Long id = 123L;

        public Long getId() {
            return id;
        }
    }
}
