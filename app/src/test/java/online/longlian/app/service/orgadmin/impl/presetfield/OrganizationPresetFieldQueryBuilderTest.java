package online.longlian.app.service.orgadmin.impl.presetfield;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldListParamsBO;
import online.longlian.app.pojo.entity.OrganizationPresetField;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationPresetFieldQueryBuilderTest {

    private final OrganizationPresetFieldQueryBuilder queryBuilder = new OrganizationPresetFieldQueryBuilder();

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationPresetField.class);
    }

    @Test
    void buildListQuery_matchesNameAndSortsByCreatedTime() {
        String descending = queryBuilder.buildListQuery(OrganizationPresetFieldListParamsBO.builder()
                .orgId(1L)
                .keyword(" 作者 ")
                .orderDir(SortDirection.DESC)
                .build()).getSqlSegment();
        String ascending = queryBuilder.buildListQuery(OrganizationPresetFieldListParamsBO.builder()
                .orgId(1L)
                .orderDir(SortDirection.ASC)
                .build()).getSqlSegment();

        assertThat(descending).contains("org_id", "name", "created_at DESC");
        assertThat(ascending).doesNotContain("name").contains("created_at ASC");
    }
}
