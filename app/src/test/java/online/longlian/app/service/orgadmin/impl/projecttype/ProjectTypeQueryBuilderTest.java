package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.entity.ProjectType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectTypeQueryBuilderTest {

    private ProjectTypeQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProjectType.class);
        queryBuilder = new ProjectTypeQueryBuilder();
    }

    @Test
    void buildListQuery_withoutKeywordAndAscending_returnsWrapper() {
        LambdaQueryWrapper<ProjectType> result = queryBuilder.buildListQuery(
                ProjectTypeListParamsBO.builder()
                        .orgId(1L)
                        .orderDir(SortDirection.ASC)
                        .build());

        assertThat(result.getSqlSegment())
                .contains("org_id")
                .contains("ORDER BY created_at ASC")
                .doesNotContain("name LIKE");
    }

    @Test
    void buildListQuery_withKeywordAndDescending_returnsWrapper() {
        LambdaQueryWrapper<ProjectType> result = queryBuilder.buildListQuery(
                ProjectTypeListParamsBO.builder()
                        .orgId(1L)
                        .keyword("  漫画  ")
                        .orderDir(SortDirection.DESC)
                        .build());

        assertThat(result.getSqlSegment())
                .contains("org_id")
                .contains("name LIKE")
                .contains("ORDER BY created_at DESC");
        assertThat(result.getParamNameValuePairs()).containsValue("%漫画%");
    }
}
