package online.longlian.app.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.common.util.OpenIdempotencySupport;
import online.longlian.app.pojo.bo.open.IdempotencyCheckParamsBO;
import online.longlian.app.pojo.bo.open.OpenFileCreateParamsBO;
import online.longlian.app.pojo.bo.open.OpenFileCreateResultBO;
import online.longlian.app.pojo.dto.open.OpenFileCreateDTO;
import online.longlian.app.pojo.vo.open.OpenFileCreateVO;
import online.longlian.app.service.open.OpenFileService;
import online.longlian.app.service.open.OpenWriteCommandService;
import online.longlian.common.enumeration.IdempotencySource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放面创建上传资源。用户与组织上下文全部来自密钥解出的 principal，不接受请求指定。
 * 返回类型为 Object：幂等回放时 data 是快照反序列化结果，不能再按 VO 强转。
 */
@RequiredArgsConstructor
@Tag(name = "开放接口-文件", description = "密钥认证的任务提交文件上传资源创建")
@SecurityRequirement(name = "XApiKey")
@RequestMapping("/open/v1/files")
@RestController
public class OpenFileController {

    private final OpenWriteCommandService openWriteCommandService;
    private final OpenFileService openFileService;

    @Operation(summary = "创建上传资源，返回预签名直传地址",
            description = "业务类型仅支持 task_submit；大小上限与类型校验同站内；"
                    + "拿到 uploadUrl 后直接向存储服务 PUT 文件内容（不走密钥），"
                    + "完成后将 fileId 传给提交接口的文件字段")
    @PostMapping
    public Object createUpload(HttpServletRequest request,
                               @AuthenticationPrincipal OpenApiPrincipal principal,
                               @Valid @RequestBody OpenFileCreateDTO dto) {
        IdempotencyCheckParamsBO checkParams =
                OpenIdempotencySupport.buildParams(request, IdempotencySource.OPEN_API, principal.keyId());
        return openWriteCommandService.execute(checkParams, () -> toVO(openFileService.createUpload(
                new OpenFileCreateParamsBO(principal.userId(), principal.orgId(),
                        dto.getFileName(), dto.getFileExt(), dto.getFileSize(), dto.getFileMime()))));
    }

    private OpenFileCreateVO toVO(OpenFileCreateResultBO bo) {
        OpenFileCreateVO vo = new OpenFileCreateVO();
        vo.setFileId(bo.fileId());
        vo.setUploadUrl(bo.uploadUrl());
        vo.setStorageType(bo.storageType());
        vo.setContentDisposition(bo.contentDisposition());
        return vo;
    }
}
