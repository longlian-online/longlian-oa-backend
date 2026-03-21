package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "工坊接口", description = "工坊相关接口")
@RequestMapping("/app/workshop")
@RestController
@RequiredArgsConstructor
public class ProjectWorkshopController {

    private final ProjectWorkshopService projectWorkshopService;
    @Operation(summary = "添加企划到工坊接口")
    @GetMapping("/add/{projectId}")
    public Result<Void> addProject(@PathVariable Long projectId) {
        //TODO
        //return projectWorkshopService.addProject(projectId);
        return Result.success("添加成功");
    }
    @Operation(summary = "工坊列表展示企划接口")
    @PostMapping("/list")
    public Result<PageResultVO<WorkshopProjectInfoVO>> getMyWorkshopList(@RequestBody @Valid WorkshopListDTO workshopListDTO) {
        //TODO
        //return projectWorkshopService.getMyWorkshopList(workshopListDTO);
        return null;

    }
}
