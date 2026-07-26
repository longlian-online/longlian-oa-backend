package online.longlian.app.service.app.impl.projectworkshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.pojo.entity.TaskTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * WorkshopQueryBuilder 单元测试。
 * <p>该类无外部依赖，所有分支均可通过直接实例化覆盖。</p>
 */
class WorkshopQueryBuilderTest {

    private WorkshopQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new WorkshopQueryBuilder();
    }

    // ---- buildProjectQuery ----

    @Test
    void buildProjectQuery_shouldReturnWrapper() {
        LambdaQueryWrapper<Project> wrapper = queryBuilder.buildProjectQuery(1L, List.of(10L, 20L));
        assertNotNull(wrapper);
    }

    // ---- buildFilteredProjectQuery ----

    @Test
    void buildFilteredProjectQuery_noOptionalParams_shouldReturnWrapper() {
        LambdaQueryWrapper<Project> wrapper = queryBuilder.buildFilteredProjectQuery(
                1L, List.of(10L), null, null, null, null);
        assertNotNull(wrapper);
    }

    @Test
    void buildFilteredProjectQuery_withKeyword_shouldApplyLike() {
        LambdaQueryWrapper<Project> wrapper = queryBuilder.buildFilteredProjectQuery(
                1L, List.of(10L), "test", null, null, null);
        assertNotNull(wrapper);
    }

    @Test
    void buildFilteredProjectQuery_withTypeId_shouldApplyEq() {
        LambdaQueryWrapper<Project> wrapper = queryBuilder.buildFilteredProjectQuery(
                1L, List.of(10L), null, 5L, null, null);
        assertNotNull(wrapper);
    }

    @Test
    void buildFilteredProjectQuery_withIsMyCreatedTrue_shouldFilterByCreator() {
        LambdaQueryWrapper<Project> wrapper = queryBuilder.buildFilteredProjectQuery(
                1L, List.of(10L), null, null, true, 99L);
        assertNotNull(wrapper);
    }

    @Test
    void buildFilteredProjectQuery_withIsMyCreatedFalse_shouldNotFilterByCreator() {
        LambdaQueryWrapper<Project> wrapper = queryBuilder.buildFilteredProjectQuery(
                1L, List.of(10L), null, null, false, 99L);
        assertNotNull(wrapper);
    }

    @Test
    void buildFilteredProjectQuery_allParamsSet_shouldReturnWrapper() {
        LambdaQueryWrapper<Project> wrapper = queryBuilder.buildFilteredProjectQuery(
                1L, List.of(10L), "keyword", 5L, true, 99L);
        assertNotNull(wrapper);
    }

    // ---- buildWorkshopListQuery ----

    @Test
    void buildWorkshopListQuery_shouldReturnWrapper() {
        LambdaQueryWrapper<ProjectWorkshop> wrapper = queryBuilder.buildWorkshopListQuery(42L);
        assertNotNull(wrapper);
    }

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
