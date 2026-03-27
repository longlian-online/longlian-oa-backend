package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.WorkshopListDTO;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.WorkshopProjectInfoVO;
import online.longlian.app.service.user.ProjectWorkshopService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "工坊接口", description = "用户个人工坊：企划列表展示")
@RequestMapping("/app/workshop")
@RestController
@RequiredArgsConstructor
public class ProjectWorkshopController {

    private final ProjectWorkshopService projectWorkshopService;

    @Operation(
        summary = "分页查询工坊企划列表",
        description = "查询当前用户工坊中的企划列表，支持标题模糊搜索、类型筛选、仅看我创建，默认按添加时间倒序。"
    )
    @PostMapping("/list")
    public Result<PageResultVO<WorkshopProjectInfoVO>> getMyWorkshopList(
            @RequestBody @Valid WorkshopListDTO workshopListDTO) {
        // TODO
        // return projectWorkshopService.getMyWorkshopList(workshopListDTO);
        return Result.success("查询成功", null);
    }
}
