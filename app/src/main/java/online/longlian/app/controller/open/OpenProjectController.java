package online.longlian.app.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.common.util.OpenQuerySupport;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenProjectBO;
import online.longlian.app.pojo.bo.open.OpenProjectListParamsBO;
import online.longlian.app.pojo.dto.open.OpenProjectListDTO;
import online.longlian.app.pojo.vo.open.OpenPageVO;
import online.longlian.app.pojo.vo.open.OpenProjectVO;
import online.longlian.app.service.open.OpenProjectService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 开放面企划查询。组织上下文全部来自密钥解出的 principal，不接受请求指定。
 */
@RequiredArgsConstructor
@Tag(name = "开放接口-企划", description = "密钥认证的企划查询")
@SecurityRequirement(name = "XApiKey")
@RequestMapping("/open/v1/projects")
@RestController
public class OpenProjectController {

    private final OpenProjectService openProjectService;

    @Operation(summary = "企划游标列表，支持 updatedSince 增量")
    @GetMapping
    public OpenPageVO<OpenProjectVO> listProjects(
            @AuthenticationPrincipal OpenApiPrincipal principal,
            @Valid OpenProjectListDTO dto) {
        OpenPageResultBO<OpenProjectBO> page = openProjectService.listProjects(
                new OpenProjectListParamsBO(
                        principal.userId(),
                        principal.orgId(),
                        OpenQuerySupport.resolveLimit(dto.getLimit()),
                        OpenQuerySupport.parseCursor(dto.getCursor()),
                        OpenQuerySupport.parseUpdatedSince(dto.getUpdatedSince())));
        List<OpenProjectVO> items = page.list().stream().map(this::toVO).toList();
        return new OpenPageVO<>(items, page.nextCursor(), page.hasMore());
    }

    private OpenProjectVO toVO(OpenProjectBO bo) {
        OpenProjectVO vo = new OpenProjectVO();
        vo.setId(bo.id());
        vo.setTitle(bo.title());
        vo.setAlias(bo.alias());
        vo.setCoverUrl(bo.coverUrl());
        vo.setThumbnailUrl(bo.thumbnailUrl());
        vo.setStatus(bo.status());
        vo.setCreator(bo.creator());
        vo.setCreatedAt(bo.createdAt());
        vo.setUpdatedAt(bo.updatedAt());
        return vo;
    }
}
