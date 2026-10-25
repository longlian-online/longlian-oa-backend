package online.longlian.app.service.app.impl.project;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMetadataValidatorTest {

    /** 可选字段不代表允许类型转换、非法颜色或未知标签字段。 */
    @ParameterizedTest
    @ValueSource(strings = {
            "{", "null", "[]", "\"text\"", "{}", "{\"tags\":null}", "{\"tags\":{}}",
            "{\"tags\":[null]}", "{\"tags\":[\"text\"]}", "{\"tags\":[{}]}",
            "{\"tags\":[{\"value\":\"smile_snow\"}]}",
            "{\"tags\":[{\"key\":\" \t\",\"value\":\"\"}]}",
            "{\"tags\":[{\"key\":1,\"value\":\"\"}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":null}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":1}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":[]}]}",
            "{\"tags\":[{\"key\":\"作者\",\"extra\":true}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":\"\",\"extra\":true}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":\"blue\",\"extra\":true}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":null}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":1}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":true}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":{}}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":[]}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":\"\"}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":\"Blue\"}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":\" blue \"}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":\"red\"}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":\"#ffffff\"}]}",
            "{\"tags\":[{\"key\":\"作者\",\"color\":\"constructor\"}]}"
    })
    void shouldRejectInvalidMetadata(String metadata) {
        assertThatThrownBy(() -> ProjectMetadataValidator.validate(metadata))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldRejectOverlongColorInsteadOfPersistingIt() {
        String metadata = "{\"tags\":[{\"key\":\"作者\",\"color\":\"" + "blue".repeat(1000) + "\"}]}";
        assertThatThrownBy(() -> ProjectMetadataValidator.validate(metadata))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
    }
}
