package online.longlian.app.service.orgadmin.impl.projecttype;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectTypeCustomFieldMapper;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldOptionBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCustomFieldBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCustomFieldResultBO;
import online.longlian.app.pojo.entity.ProjectTypeCustomField;
import online.longlian.app.service.orgadmin.OrganizationPresetFieldService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectTypeCustomFormServiceImplTest {

    @Mock
    private ProjectTypeCustomFieldMapper projectTypeCustomFieldMapper;
    @Mock
    private OrganizationPresetFieldService organizationPresetFieldService;

    private ProjectTypeCustomFormServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProjectTypeCustomField.class);
        service = new ProjectTypeCustomFormServiceImpl(
                projectTypeCustomFieldMapper,
                organizationPresetFieldService,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void replace_keepsExistingFormWhenFieldsAreOmitted() {
        service.replace(1L, 10L, null);

        verify(projectTypeCustomFieldMapper, never()).delete(any());
        verify(projectTypeCustomFieldMapper, never()).insert(any(ProjectTypeCustomField.class));
    }

    @Test
    void replace_rejectsDuplicatePresetFields() {
        assertThatThrownBy(() -> service.replace(1L, 10L, List.of(
                ProjectTypeCustomFieldBO.builder().presetFieldId(1L).required(true).build(),
                ProjectTypeCustomFieldBO.builder().presetFieldId(1L).required(false).build())))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));
        verify(projectTypeCustomFieldMapper, never()).delete(any());
    }

    @Test
    void replace_rejectsPresetFieldsOutsideTheOrganization() {
        when(organizationPresetFieldService.listExisting(1L, List.of(9L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.replace(1L, 10L, List.of(
                ProjectTypeCustomFieldBO.builder().presetFieldId(9L).required(true).build())))
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getMessage()).isEqualTo("预置表单字段不存在"));
    }

    @Test
    void replace_rewritesRowsInRequestOrder() {
        when(organizationPresetFieldService.listExisting(1L, List.of(2L, 1L))).thenReturn(List.of(
                OrganizationPresetFieldOptionBO.builder().id(1L).name("状态").build(),
                OrganizationPresetFieldOptionBO.builder().id(2L).name("作者").build()));

        service.replace(1L, 10L, List.of(
                ProjectTypeCustomFieldBO.builder().presetFieldId(2L).required(true).build(),
                ProjectTypeCustomFieldBO.builder().presetFieldId(1L).required(false).build()));

        verify(projectTypeCustomFieldMapper).delete(any(LambdaQueryWrapper.class));
        ArgumentCaptor<ProjectTypeCustomField> captor = ArgumentCaptor.forClass(ProjectTypeCustomField.class);
        verify(projectTypeCustomFieldMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(ProjectTypeCustomField::getPresetFieldId).containsExactly(2L, 1L);
        assertThat(captor.getAllValues()).extracting(ProjectTypeCustomField::getRequired).containsExactly(true, false);
        assertThat(captor.getAllValues()).extracting(ProjectTypeCustomField::getSort).containsExactly(0, 1);
    }

    @Test
    void listByTypeIds_skipsPresetFieldsThatNoLongerExist() {
        when(projectTypeCustomFieldMapper.selectList(any())).thenReturn(List.of(
                ProjectTypeCustomField.builder().typeId(10L).presetFieldId(1L).required(true).sort(0).build(),
                ProjectTypeCustomField.builder().typeId(10L).presetFieldId(9L).required(false).sort(1).build()));
        when(organizationPresetFieldService.listExisting(1L, List.of(1L, 9L))).thenReturn(List.of(
                OrganizationPresetFieldOptionBO.builder().id(1L).name("作者").build()));

        Map<Long, List<ProjectTypeCustomFieldResultBO>> forms = service.listByTypeIds(1L, List.of(10L));

        assertThat(forms.get(10L)).extracting(ProjectTypeCustomFieldResultBO::getName).containsExactly("作者");
        assertThat(service.requiredFieldNames(1L, 10L)).containsExactly("作者");
        assertThat(service.listByTypeIds(1L, List.of())).isEmpty();
    }

    @Test
    void deleteByType_removesTheFormsRows() {
        service.deleteByType(1L, 10L);

        verify(projectTypeCustomFieldMapper).delete(any(LambdaQueryWrapper.class));
    }
}
