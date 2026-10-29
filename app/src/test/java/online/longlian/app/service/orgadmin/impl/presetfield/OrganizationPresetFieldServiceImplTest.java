package online.longlian.app.service.orgadmin.impl.presetfield;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationPresetFieldMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetFieldListParamsBO;
import online.longlian.app.pojo.entity.OrganizationPresetField;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.orgadmin.ProjectTypeCustomFormService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationPresetFieldServiceImplTest {

    @Mock
    private OrganizationPresetFieldMapper organizationPresetFieldMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OrganizationPresetFieldQueryBuilder queryBuilder;
    @Mock
    private ObjectProvider<ProjectTypeCustomFormService> projectTypeCustomFormService;
    @Mock
    private ProjectTypeCustomFormService customFormService;

    private OrganizationPresetFieldServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationPresetField.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new OrganizationPresetFieldServiceImpl(
                organizationPresetFieldMapper, userMapper, clock, queryBuilder, projectTypeCustomFormService);
    }

    @Test
    void createPresetField_trimsName() {
        service.createPresetField(OrganizationPresetFieldCreateParamsBO.builder()
                .orgId(1L)
                .creatorId(2L)
                .name("  作者  ")
                .build());

        ArgumentCaptor<OrganizationPresetField> captor = ArgumentCaptor.forClass(OrganizationPresetField.class);
        verify(organizationPresetFieldMapper).insert(captor.capture());
        OrganizationPresetField field = captor.getValue();
        assertThat(field.getOrgId()).isEqualTo(1L);
        assertThat(field.getCreatorId()).isEqualTo(2L);
        assertThat(field.getName()).isEqualTo("作者");
        assertThat(field.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void deletePresetField_removesFieldAndDetachesTypeForms() {
        when(organizationPresetFieldMapper.selectById(8L)).thenReturn(OrganizationPresetField.builder()
                .id(8L).orgId(1L).name("作者").build());
        when(projectTypeCustomFormService.getObject()).thenReturn(customFormService);

        service.deletePresetField(OrganizationPresetFieldDeleteParamsBO.builder().fieldId(8L).orgId(1L).build());

        verify(organizationPresetFieldMapper).deleteById(8L);
        verify(customFormService).detachPresetField(1L, 8L);
    }

    @Test
    void listPresetFields_usesNullNicknameWhenCreatorHasNoDisplayName() {
        OrganizationPresetField field = OrganizationPresetField.builder()
                .id(1L).orgId(1L).name("作者").creatorId(2L).build();
        Page<OrganizationPresetField> page = new Page<>(1, 10);
        page.setRecords(List.of(field));
        page.setTotal(1);
        when(queryBuilder.buildListQuery(any())).thenReturn(new LambdaQueryWrapper<>());
        when(organizationPresetFieldMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(List.of(2L))).thenReturn(List.of(User.builder().id(2L).nickname("  ").build()));

        var result = service.listPresetFields(OrganizationPresetFieldListParamsBO.builder()
                .orgId(1L).page(new PageParamsBO(1, 10)).build());

        assertThat(result.getList()).singleElement().satisfies(item -> {
            assertThat(item.getName()).isEqualTo("作者");
            assertThat(item.getCreatorNickname()).isNull();
        });
    }

    @Test
    void deletePresetField_rejectsAnotherOrganization() {
        when(organizationPresetFieldMapper.selectById(8L)).thenReturn(OrganizationPresetField.builder()
                .id(8L).orgId(2L).name("作者").build());

        assertThatThrownBy(() -> service.deletePresetField(OrganizationPresetFieldDeleteParamsBO.builder()
                .fieldId(8L).orgId(1L).build()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }
}
