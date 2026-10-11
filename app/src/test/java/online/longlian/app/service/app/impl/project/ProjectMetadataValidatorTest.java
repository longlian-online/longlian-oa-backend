package online.longlian.app.service.app.impl.project;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMetadataValidatorTest {

    /** 拒绝错误 JSON、非对象、错误 tags、缺失字段、类型转换及额外 tag 字段。 */
    @ParameterizedTest
    @ValueSource(strings = {
            "{", "null", "[]", "\"text\"", "{}", "{\"tags\":null}", "{\"tags\":{}}",
            "{\"tags\":[null]}", "{\"tags\":[\"text\"]}", "{\"tags\":[{}]}",
            "{\"tags\":[{\"key\":\"作者\"}]}", "{\"tags\":[{\"value\":\"smile_snow\"}]}",
            "{\"tags\":[{\"key\":\" \t\",\"value\":\"\"}]}",
            "{\"tags\":[{\"key\":1,\"value\":\"\"}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":null}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":1}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":[]}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":\"\",\"extra\":true}]}"
    })
    void shouldRejectInvalidMetadata(String metadata) {
        assertThatThrownBy(() -> ProjectMetadataValidator.validate(metadata))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }
}
