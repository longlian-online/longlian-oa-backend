package online.longlian.app.service.productrelease;

import online.longlian.app.common.exception.AppException;
import org.junit.jupiter.api.Test;

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
                "  # 更新\n\n- 一条新增  ",
                true
        );

        assertThat(content.version()).isEqualTo("1.2.3");
        assertThat(content.frontendVersion()).isEqualTo("0.0.18");
        assertThat(content.backendVersion()).isEqualTo("1.0.52");
        assertThat(content.title()).isEqualTo("标题");
        assertThat(content.summary()).isEqualTo("摘要");
        assertThat(content.content()).isEqualTo("# 更新\n\n- 一条新增");
    }

    @Test
    void shouldRejectBlankSummaryWhenPublishing() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "  ", "一条", true))
                .isInstanceOf(AppException.class)
                .hasMessage("摘要不能为空");
    }

    @Test
    void shouldRejectBlankContentWhenPublishing() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘要", "  ", true))
                .isInstanceOf(AppException.class)
                .hasMessage("更新说明不能为空");
    }

    @Test
    void shouldRejectSnapshotAndInvalidVersions() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0-SNAPSHOT", "1.0.0", "1.0.0", "标题", "摘要", "一条", true))
                .isInstanceOf(AppException.class)
                .hasMessage("版本格式不正确");

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                null, "0.0.18", "1.0.52", "标题", "摘要", "一条", false))
                .isInstanceOf(AppException.class)
                .hasMessage("版本格式不正确");
    }

    @Test
    void shouldRejectNullAndOversizedVersions() {
        assertThatThrownBy(() -> ProductReleaseRules.normalizeVersion(null))
                .isInstanceOf(AppException.class)
                .hasMessage("版本格式不正确");

        assertThatThrownBy(() -> ProductReleaseRules.normalizeVersion("1.0"))
                .isInstanceOf(AppException.class)
                .hasMessage("版本格式不正确");

        assertThatThrownBy(() -> ProductReleaseRules.normalizeVersion("01.0.0"))
                .isInstanceOf(AppException.class)
                .hasMessage("版本格式不正确");

        assertThat(ProductReleaseRules.normalizeVersion("V1.0.0")).isEqualTo("1.0.0");
    }

    @Test
    void shouldRejectOversizedCopy() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标".repeat(41), "摘要", "一条", false))
                .isInstanceOf(AppException.class)
                .hasMessage("标题最多 40 字");

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘".repeat(121), "一条", false))
                .isInstanceOf(AppException.class)
                .hasMessage("摘要最多 120 字");

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘要", "字".repeat(4001), false))
                .isInstanceOf(AppException.class)
                .hasMessage("更新说明最多 4000 字");
    }

    @Test
    void shouldAllowEmptyDraftCopy() {
        ProductReleaseContent content = ProductReleaseRules.normalize(
                "1.0.0", "0.0.1", "1.0.1", null, null, null, false);

        assertThat(content.title()).isEmpty();
        assertThat(content.summary()).isEmpty();
        assertThat(content.content()).isEmpty();
        assertThat(content.samePair("0.0.1", "1.0.1")).isTrue();
        assertThat(content.samePair("0.0.2", "1.0.1")).isFalse();
        assertThat(content.samePair("0.0.1", "1.0.2")).isFalse();
    }
}
