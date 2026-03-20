package online.longlian.app.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.dto.CreateFileReqDTO;
import online.longlian.app.pojo.vo.CreateFileResVO;
import online.longlian.app.service.resource.FileStorageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 文件存储/上传接口
 */
@RestController
@RequestMapping("/app/file")
@RequiredArgsConstructor
@Tag(name = "文件上传接口")
@Validated
public class FileStorageController {

    private final FileStorageService fileStorageService;

    /**
     * 创建文件上传（获取预签名上传地址）
     */
    @Operation(summary = "创建文件上传", description = "获取文件预签名上传地址")
    @PostMapping("/upload")
    public CreateFileResVO createFileUpload(@RequestBody @Valid CreateFileReqDTO createFileReqDTO) {
        return fileStorageService.create(createFileReqDTO);
    }

}