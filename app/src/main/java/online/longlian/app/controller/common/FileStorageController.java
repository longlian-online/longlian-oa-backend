package online.longlian.app.controller.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.annotation.ResponseMessage;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.dto.common.CreateFileReqDTO;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "文件上传接口", description = "文件预签名上传(用户端/管理端共用)，支持本地存储/OSS/COS")
@RequestMapping("/common/file")
@RestController
@RequiredArgsConstructor
public class FileStorageController {

    private final ResourceService resourceService;

    @Operation(
        summary = "创建文件上传",
        description = "获取预签名上传地址，前端拿到 uploadUrl 后直接向存储服务上传文件，上传完成后将 fileId 传给业务接口"
    )
    @PostMapping("/upload")
    @ResponseMessage("获取成功")
    public ResourceCreateVO createFileUpload(@RequestBody @Valid CreateFileReqDTO createFileReqDTO,
                                             @UserSession SessionContext sessionContext) {
        ResourceCreateParamsBO params = new ResourceCreateParamsBO(
                sessionContext.userId(),
                sessionContext.orgId(),
                createFileReqDTO.getFileName(),
                createFileReqDTO.getFileExt(),
                createFileReqDTO.getFileSize(),
                createFileReqDTO.getFileMime(),
                createFileReqDTO.getBizType(),
                createFileReqDTO.getBizId()
        );
        return resourceService.create(params);
    }

    @PutMapping("/local")
    @ResponseMessage("上传成功")
    public void uploadLocalFile(@RequestParam String key,
                                @RequestBody byte[] content,
                                @UserSession SessionContext sessionContext) {
        resourceService.uploadLocalResource(key, content, sessionContext.userId(), sessionContext.orgId());
    }

    @GetMapping("/local")
    public ResponseEntity<Resource> readLocalFile(@RequestParam String key) {
        return ResponseEntity.ok(resourceService.getLocalResource(key));
    }
}
