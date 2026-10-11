package online.longlian.app.service.app.impl.project;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.entity.Project;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectQueryBuilderTest {

    private ProjectQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), Project.class);
        queryBuilder = new ProjectQueryBuilder();
    }

    @Test
    void buildListQuery_alwaysFiltersEnabledProjects() {
        LambdaQueryWrapper<Project> result = queryBuilder.buildListQuery(
                ProjectListParamsBO.builder()
                        .orgId(1L)
                        .sortByTime(SortByTime.CREATE)
                        .orderDir(SortDirection.DESC)
                        .build(),
                null
        );

        assertThat(result.getSqlSegment()).contains("resource_status");
        assertThat(result.getParamNameValuePairs()).containsValue(Status.ENABLED);
    }
}
