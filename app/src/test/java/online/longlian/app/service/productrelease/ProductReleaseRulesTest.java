package online.longlian.app.service.productrelease;

import online.longlian.app.common.exception.AppException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

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
    void shouldRejectBlankSummaryWhenPublishing() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "  ", List.of("一条"), List.of(), List.of(), true))
                .isInstanceOf(AppException.class)
                .hasMessage("摘要不能为空");
    }

    @Test
    void shouldRejectMissingChangeWhenPublishing() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘要", List.of(), List.of(), List.of(), true))
                .isInstanceOf(AppException.class)
                .hasMessage("至少写一条变更");
    }

    @Test
    void shouldRejectBlankChangeItemWhenPublishing() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘要", List.of(" "), List.of(), List.of(), true))
                .isInstanceOf(AppException.class)
                .hasMessage("新增不能有空白条目");
    }

    @Test
    void shouldRejectSnapshotAndInvalidVersions() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0-SNAPSHOT", "1.0.0", "1.0.0", "标题", "摘要", List.of("一条"), List.of(), List.of(), true))
                .isInstanceOf(AppException.class)
                .hasMessage("版本格式不正确");

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                null, "0.0.18", "1.0.52", "标题", "摘要", List.of("一条"), List.of(), List.of(), false))
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
                "1.0.0", "1.0.0", "1.0.0", "标".repeat(41), "摘要", List.of("一条"), List.of(), List.of(), false))
                .isInstanceOf(AppException.class)
                .hasMessage("标题最多 40 字");

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘".repeat(121), List.of("一条"), List.of(), List.of(), false))
                .isInstanceOf(AppException.class)
                .hasMessage("摘要最多 120 字");
    }

    @Test
    void shouldRejectTooManyChangeItems() {
        List<String> tooMany = IntStream.rangeClosed(1, 21).mapToObj(index -> "第" + index + "条").toList();

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘要", tooMany, List.of(), List.of(), false))
                .isInstanceOf(AppException.class)
                .hasMessage("新增最多 20 条");

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘要", List.of(), List.of(), tooMany, false))
                .isInstanceOf(AppException.class)
                .hasMessage("修复最多 20 条");
    }

    @Test
    void shouldRejectNullAndOversizedChangeItem() {
        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘要", List.of(), List.of(), java.util.Arrays.asList((String) null), false))
                .isInstanceOf(AppException.class)
                .hasMessage("修复不能有空白条目");

        assertThatThrownBy(() -> ProductReleaseRules.normalize(
                "1.0.0", "1.0.0", "1.0.0", "标题", "摘要", List.of("条".repeat(81)), List.of(), List.of(), false))
                .isInstanceOf(AppException.class)
                .hasMessage("新增每条最多 80 字");
    }

    @Test
    void shouldAllowEmptyDraftCopy() {
        ProductReleaseContent content = ProductReleaseRules.normalize(
                "1.0.0", "0.0.1", "1.0.1", null, null, null, null, null, false);

        assertThat(content.title()).isEmpty();
        assertThat(content.summary()).isEmpty();
        assertThat(content.hasChange()).isFalse();
        assertThat(content.samePair("0.0.1", "1.0.1")).isTrue();
        assertThat(content.samePair("0.0.2", "1.0.1")).isFalse();
        assertThat(content.samePair("0.0.1", "1.0.2")).isFalse();
    }

    @Test
    void shouldReportChangePresencePerGroup() {
        ProductReleaseContent changed = ProductReleaseRules.normalize(
                "1.0.0", "0.0.1", "1.0.1", "标题", "摘要", List.of(), List.of("一条调整"), List.of(), false);
        ProductReleaseContent fixed = ProductReleaseRules.normalize(
                "1.0.0", "0.0.1", "1.0.1", "标题", "摘要", List.of(), List.of(), List.of("一条修复"), false);

        assertThat(changed.hasChange()).isTrue();
        assertThat(fixed.hasChange()).isTrue();
    }
}
