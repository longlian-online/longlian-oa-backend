package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.ProjectCreateDTO;
import online.longlian.app.pojo.dto.ProjectListDTO;
import online.longlian.app.pojo.dto.ProjectUpdateDTO;
import online.longlian.app.pojo.vo.PageResultVO;
import online.longlian.app.pojo.vo.ProjectInfoVO;
import online.longlian.app.pojo.vo.ProjectTypeInfoVO;
import online.longlian.app.service.user.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "企划接口", description = "企划相关接口")
@RequestMapping("/app/project")
@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    @Operation(summary = "分页查询企划列表", description = "支持关键词搜索、类型筛选、时间筛选")
    @PostMapping("/list")
    public Result<PageResultVO<ProjectInfoVO>> getProjectInfo(@RequestBody @Valid ProjectListDTO projectListDTO) {
        //TODO
        //return projectService.getProjectInfo(projectDTO);
        return null;
    }
    @PostMapping("/create")
    public Result<Void> createProject(@RequestBody ProjectCreateDTO projectCreateDTO) {
        //TODO
        //return projectService.createProject(projectCreateDTO);
        return Result.success("创建成功");
    }
    @PutMapping("/update")
    public Result<Void> updateProject(@RequestBody ProjectUpdateDTO projectUpdateDTO) {
        //TODO
        //return projectService.updateProject(projectUpdateDTO);
        return Result.success("修改成功");
    }
    @GetMapping("/type")
    public Result<List<ProjectTypeInfoVO>> getProjectType() {
        //TODO
        //return projectService.getProjectType();
        return null;
    }
}
