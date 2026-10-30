package online.longlian.app.service.productrelease;

import online.longlian.app.common.exception.AppException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductReleaseRulesTest {

    @Test
    void shouldStripVersionPrefixAndTrimText() {
        ProductReleaseContent content = ProductReleaseRules.normalize(
                " v1.2.3 ",
                "V0.0.18",
                "1.0.52",
                " 标题 ",
                " 摘要 ",
                List.of(" 一条新增 "),
                List.of(),
                null,
                true
        );

        assertThat(content.version()).isEqualTo("1.2.3");
        assertThat(content.frontendVersion()).isEqualTo("0.0.18");
        assertThat(content.backendVersion()).isEqualTo("1.0.52");
        assertThat(content.title()).isEqualTo("标题");
        assertThat(content.added()).containsExactly("一条新增");
        assertThat(content.fixed()).isEmpty();
    }

    @Test
    void shouldRejectSnapshotAndBlankChangeWhenPublishing() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0-SNAPSHOT", "1.0.0", "1.0.0", "标题", "摘要", List.of("一条"), List.of(), List.of(), true))
                .isInstanceOf(AppException.class)
                .hasMessage("版本格式不正确");

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "  ", "摘要", List.of(), List.of(), List.of(), true))
                .isInstanceOf(AppException.class)
                .hasMessage("标题不能为空");

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘要", List.of(" "), List.of(), List.of(), false))
                .isInstanceOf(AppException.class)
                .hasMessage("新增不能有空白条目");
    }

    @Test
    void shouldAllowEmptyDraftCopy() {
        ProductReleaseContent content = ProductReleaseRules.normalize(
                "1.0.0", "0.0.1", "1.0.1", null, null, null, null, null, false);

        assertThat(content.title()).isEmpty();
        assertThat(content.summary()).isEmpty();
        assertThat(content.hasChange()).isFalse();
    }
}
