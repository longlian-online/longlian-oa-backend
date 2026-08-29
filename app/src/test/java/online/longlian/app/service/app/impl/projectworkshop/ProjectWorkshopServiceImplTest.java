package online.longlian.app.service.app.impl.projectworkshop;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.ProjectWorkshopMapper;
import online.longlian.app.mapper.TaskTemplateMapper;
import online.longlian.app.mapper.TaskTemplateNodeMapper;
import online.longlian.app.pojo.bo.app.WorkshopListParamsBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.vo.app.WorkshopProjectInfoVO;
import online.longlian.app.service.common.LockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectWorkshopServiceImplTest {

    @Mock
    private ProjectWorkshopMapper projectWorkshopMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private TaskTemplateMapper taskTemplateMapper;
    @Mock
    private TaskTemplateNodeMapper taskTemplateNodeMapper;
    @Mock
    private WorkshopQueryBuilder workshopQueryBuilder;
    @Mock
    private WorkshopAssembler workshopAssembler;
    @Mock
    private WorkshopProjectHandler workshopProjectHandler;
    @Mock
    private LockService lockService;

    private ProjectWorkshopServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProjectWorkshopServiceImpl(
                projectWorkshopMapper,
                projectMapper,
                taskTemplateMapper,
                taskTemplateNodeMapper,
                Clock.systemUTC(),
                workshopQueryBuilder,
                workshopAssembler,
                workshopProjectHandler,
                lockService);
    }

    @Test
    void getMyWorkshopList_unknownProjectType_validatesBeforeWorkshopQuery() {
        WorkshopListParamsBO params = buildListParams("不存在的类型");
        when(workshopProjectHandler.resolveTypeId(1L, "不存在的类型"))
                .thenThrow(new AppException(ResultCode.PARAM_ERROR, "企划类型不存在或已禁用"));

        assertThatThrownBy(() -> service.getMyWorkshopList(params))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode()));

        verify(projectWorkshopMapper, never()).selectList(any());
    }

    @Test
    void getMyWorkshopList_withoutProjectTypeAndWorkshops_returnsEmptyPage() {
        WorkshopListParamsBO params = buildListParams(null);
        when(workshopProjectHandler.resolveTypeId(1L, null)).thenReturn(null);
        when(projectWorkshopMapper.selectList(any())).thenReturn(Collections.emptyList());

        PageResultBO<WorkshopProjectInfoVO> result = service.getMyWorkshopList(params);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotal()).isZero();
        verify(projectWorkshopMapper).selectList(any());
    }

    private WorkshopListParamsBO buildListParams(String projectType) {
        return WorkshopListParamsBO.builder()
                .userId(2L)
                .orgId(1L)
                .projectType(projectType)
                .page(new PageParamsBO(1, 10))
                .build();
    }
}
