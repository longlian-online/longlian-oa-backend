package online.longlian.app.service.orgadmin.impl.presettag;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationPresetTagMapper;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagListResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrganizationPresetTagUpdateParamsBO;
import online.longlian.app.pojo.entity.OrganizationPresetTag;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class OrganizationPresetTagServiceImplTest {

    @Mock
    private OrganizationPresetTagMapper organizationPresetTagMapper;
    @Mock
    private OrganizationPresetTagQueryBuilder queryBuilder;

    private OrganizationPresetTagServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationPresetTag.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new OrganizationPresetTagServiceImpl(organizationPresetTagMapper, clock, queryBuilder);
    }

    @Test
    void createPresetTag_trimsName_defaultsPriority_andStoresNullColor() {
        service.createPresetTag(OrganizationPresetTagCreateParamsBO.builder()
                .orgId(1L)
                .creatorId(2L)
                .name("  加急  ")
                .color("  ")
                .priority(null)
                .build());

        ArgumentCaptor<OrganizationPresetTag> captor = ArgumentCaptor.forClass(OrganizationPresetTag.class);
        verify(organizationPresetTagMapper).insert(captor.capture());
        OrganizationPresetTag tag = captor.getValue();
        assertThat(tag.getOrgId()).isEqualTo(1L);
        assertThat(tag.getCreatorId()).isEqualTo(2L);
        assertThat(tag.getName()).isEqualTo("加急");
        assertThat(tag.getColor()).isNull();
        assertThat(tag.getPriority()).isZero();
        assertThat(tag.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void createPresetTag_rejectsUnknownColor() {
        assertThatThrownBy(() -> service.createPresetTag(OrganizationPresetTagCreateParamsBO.builder()
                .orgId(1L)
                .creatorId(2L)
                .name("加急")
                .color("red")
                .priority(0)
                .build()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void updatePresetTag_rejectsTagFromAnotherOrganization() {
        when(organizationPresetTagMapper.selectById(8L)).thenReturn(OrganizationPresetTag.builder()
                .id(8L)
                .orgId(9L)
                .name("作者")
                .build());

        assertThatThrownBy(() -> service.updatePresetTag(OrganizationPresetTagUpdateParamsBO.builder()
                .tagId(8L)
                .orgId(1L)
                .name("作者")
                .build()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
    }

    @Test
    void deletePresetTag_rejectsMissingTag() {
        when(organizationPresetTagMapper.selectById(8L)).thenReturn(null);

        assertThatThrownBy(() -> service.deletePresetTag(OrganizationPresetTagDeleteParamsBO.builder()
                .tagId(8L)
                .orgId(1L)
                .build()))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).getCode())
                .isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
    }

    @Test
    void listPresetTags_returnsMappedPage() {
        OrganizationPresetTagListParamsBO params = OrganizationPresetTagListParamsBO.builder()
                .orgId(1L)
                .page(new PageParamsBO(1, 10))
                .build();
        LambdaQueryWrapper<OrganizationPresetTag> queryWrapper = new LambdaQueryWrapper<>();
        Page<OrganizationPresetTag> page = new Page<>(1, 10);
        page.setTotal(1L);
        page.setRecords(List.of(OrganizationPresetTag.builder()
                .id(3L)
                .name("作者")
                .color("blue")
                .priority(10)
                .build()));
        when(queryBuilder.buildListQuery(params)).thenReturn(queryWrapper);
        when(organizationPresetTagMapper.selectPage(any(), any())).thenReturn(page);

        PageResultBO<OrganizationPresetTagListResultBO> result = service.listPresetTags(params);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).singleElement().satisfies(tag -> {
            assertThat(tag.getId()).isEqualTo(3L);
            assertThat(tag.getName()).isEqualTo("作者");
            assertThat(tag.getColor()).isEqualTo("blue");
            assertThat(tag.getPriority()).isEqualTo(10);
        });
    }

}
