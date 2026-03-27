package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.CreateFileReqDTO;
import online.longlian.app.pojo.vo.CreateFileResVO;
import online.longlian.app.service.resource.FileStorageService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "文件上传接口", description = "文件预签名上传，支持本地存储/OSS/COS")
@RequestMapping("/app/file")
@RestController
@RequiredArgsConstructor
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @Operation(
        summary = "创建文件上传",
        description = "获取预签名上传地址，前端拿到 uploadUrl 后直接向存储服务上传文件，上传完成后将 fileId 传给业务接口"
    )
    @PostMapping("/upload")
    public Result<CreateFileResVO> createFileUpload(@RequestBody @Valid CreateFileReqDTO createFileReqDTO) {
        return Result.success("获取成功", fileStorageService.create(createFileReqDTO));
    }
}
