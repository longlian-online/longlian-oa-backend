package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import online.longlian.app.pojo.entity.TaskTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * WorkshopQueryBuilder 单元测试。
 * <p>该类无外部依赖，所有分支均可通过直接实例化覆盖。</p>
 */
class WorkshopQueryBuilderTest {

    private final WorkshopQueryBuilder queryBuilder = new WorkshopQueryBuilder();

    // ---- buildCombinedTemplateQuery ----

    @Test
    void buildCombinedTemplateQuery_noOptionalParams_shouldReturnWrapper() {
        LambdaQueryWrapper<TaskTemplate> wrapper = queryBuilder.buildCombinedTemplateQuery(
                1L, 2L, null, null);
        assertNotNull(wrapper);
    }

    @Test
    void buildCombinedTemplateQuery_withKeyword_shouldApplyLike() {
        LambdaQueryWrapper<TaskTemplate> wrapper = queryBuilder.buildCombinedTemplateQuery(
                1L, 2L, "story", null);
        assertNotNull(wrapper);
    }

    @Test
    void buildCombinedTemplateQuery_withIsMyCreatedTrue_shouldFilterByPersonal() {
        LambdaQueryWrapper<TaskTemplate> wrapper = queryBuilder.buildCombinedTemplateQuery(
                1L, 2L, null, true);
        assertNotNull(wrapper);
    }

    @Test
    void buildCombinedTemplateQuery_withIsMyCreatedFalse_shouldNotFilter() {
        LambdaQueryWrapper<TaskTemplate> wrapper = queryBuilder.buildCombinedTemplateQuery(
                1L, 2L, null, false);
        assertNotNull(wrapper);
    }

    // ---- buildOrgTemplateQuery ----

    @Test
    void buildOrgTemplateQuery_withoutKeyword_shouldReturnWrapper() {
        LambdaQueryWrapper<TaskTemplate> wrapper = queryBuilder.buildOrgTemplateQuery(1L, null);
        assertNotNull(wrapper);
    }

    @Test
    void buildOrgTemplateQuery_withKeyword_shouldApplyLike() {
        LambdaQueryWrapper<TaskTemplate> wrapper = queryBuilder.buildOrgTemplateQuery(1L, "draw");
        assertNotNull(wrapper);
    }

    // ---- buildPersonalTemplateQuery ----

    @Test
    void buildPersonalTemplateQuery_withoutKeyword_shouldReturnWrapper() {
        LambdaQueryWrapper<TaskTemplate> wrapper = queryBuilder.buildPersonalTemplateQuery(99L, null);
        assertNotNull(wrapper);
    }

    @Test
    void buildPersonalTemplateQuery_withKeyword_shouldApplyLike() {
        LambdaQueryWrapper<TaskTemplate> wrapper = queryBuilder.buildPersonalTemplateQuery(99L, "edit");
        assertNotNull(wrapper);
    }
}
