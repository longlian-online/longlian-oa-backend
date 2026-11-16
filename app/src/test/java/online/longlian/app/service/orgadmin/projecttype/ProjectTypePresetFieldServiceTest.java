package online.longlian.app.service.orgadmin.projecttype;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.ProjectTypeCustomFieldMapper;
import online.longlian.app.pojo.entity.ProjectTypeCustomField;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProjectTypePresetFieldServiceTest {

    @Test
    void shouldDetachOnlyTheRequestedOrganizationsPresetField() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ProjectTypeCustomField.class);
        ProjectTypeCustomFieldMapper mapper = mock(ProjectTypeCustomFieldMapper.class);

        new ProjectTypePresetFieldService(mapper).detachPresetField(1L, 8L);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<LambdaQueryWrapper> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).delete(query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("org_id", "preset_field_id");
        assertThat(query.getValue().getParamNameValuePairs()).containsValues(1L, 8L);
    }
}
