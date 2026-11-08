package online.longlian.app.mcp;

import com.alibaba.fastjson2.JSON;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.ItemSortBy;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.McpTaskQueryParamsBO;
import online.longlian.app.pojo.bo.app.McpTaskQueryResultBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadParamsBO;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.TaskAttachmentDownloadParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.dto.app.TaskSubmitDTO;
import online.longlian.app.pojo.dto.common.CreateFileReqDTO;
import online.longlian.app.service.app.ItemService;
import online.longlian.app.service.app.McpTaskQueryService;
import online.longlian.app.service.app.ProjectAttachmentService;
import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.app.service.resource.ResourceService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskTools {

    private final McpUserSession userSession;
    private final ProjectService projectService;
    private final ItemService itemService;
    private final McpTaskQueryService taskQueryService;
    private final TaskInstanceService taskInstanceService;
    private final ProjectAttachmentService projectAttachmentService;
    private final ResourceService resourceService;
    private final Validator validator;

    @McpTool(name = "find_projects", description = "按名称或别名查找当前组织的企划。projectId 是企划 ID，"
            + "用 list_project_items 查找其中的具体项目；重名时请用户选定，不自行猜测。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public String findProjects(McpSyncServerExchange exchange,
            @McpToolParam(description = "企划名称或别名关键词", required = false) String keyword,
            @McpToolParam(description = "页码，从 1 开始，默认 1", required = false) Integer pageNum,
            @McpToolParam(description = "每页数量，1 到 100，默认 20", required = false) Integer pageSize) {
        SessionContext session = userSession.require(exchange);
        var result = projectService.getProjectList(ProjectListParamsBO.builder().orgId(session.orgId())
                .keyword(keyword).sortByTime(SortByTime.CREATE).orderDir(SortDirection.ASC)
                .page(page(pageNum, pageSize)).build());
        return JSON.toJSONString(new McpTaskQueryResultBO.Page<>(result.getList().stream()
                .map(value -> new McpTaskQueryResultBO.Project(value.getId().toString(), value.getTitle(),
                        value.getAlias(), value.getDescription())).toList(), result.getTotal()));
    }

    @McpTool(name = "list_project_items", description = "查找指定企划下的项目，按网页自定义顺序分页返回。"
            + "用返回的 projectId、itemId 查询任务流。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public String listProjectItems(McpSyncServerExchange exchange,
            @McpToolParam(description = "企划 ID，十进制字符串") String projectId,
            @McpToolParam(description = "项目名称关键词", required = false) String keyword,
            @McpToolParam(description = "页码，从 1 开始，默认 1", required = false) Integer pageNum,
            @McpToolParam(description = "每页数量，1 到 100，默认 20", required = false) Integer pageSize) {
        SessionContext session = userSession.require(exchange);
        var result = itemService.listProjectItems(ItemListParamsBO.builder().orgId(session.orgId())
                .projectId(McpUserSession.positiveId(projectId)).keyword(keyword).sortBy(ItemSortBy.MANUAL)
                .page(page(pageNum, pageSize)).build());
        return JSON.toJSONString(new McpTaskQueryResultBO.Page<>(result.getList().stream()
                .map(value -> new McpTaskQueryResultBO.Item(value.getId().toString(), value.getTitle(),
                        value.getProgressPercent(), value.getCurrentNodeName())).toList(), result.getTotal()));
    }

    @McpTool(name = "get_project_tasks", description = "读取项目任务流，按 sort、parallelSort 顺序返回。"
            + "同 sort 为并行任务。只处理 canClaim 或 canWork 的任务；有草稿时先读取再修改。"
            + "暂存不会解锁下一阶段，没有可执行任务时向用户汇报待审核项并停止；人工网页提交后再查询继续。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public String getProjectTasks(McpSyncServerExchange exchange,
            @McpToolParam(description = "企划 ID") String projectId,
            @McpToolParam(description = "项目 ID") String itemId) {
        return JSON.toJSONString(taskQueryService.getProjectTasks(query(exchange, projectId, itemId, null)));
    }

    @McpTool(name = "get_task", description = "读取任务执行上下文：要求、提交字段、企划参考附件、前序正式产出、"
            + "当前用户的草稿。只做用户指定范围的任务；材料不足时询问用户。所有 ID 都用字符串。"
            + "修改前先读取 draftValues，保存时传完整字段，保留未修改的值与 fileId。"
            + "文本、数字、选项字段均传字符串，文件字段传 {fileId:十进制字符串}。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, openWorldHint = false))
    public String getTask(McpSyncServerExchange exchange,
            @McpToolParam(description = "企划 ID") String projectId,
            @McpToolParam(description = "项目 ID") String itemId,
            @McpToolParam(description = "任务实例 ID") String instanceId) {
        return JSON.toJSONString(taskQueryService.getTask(query(exchange, projectId, itemId, instanceId)));
    }

    @McpTool(name = "claim_task", description = "以当前用户本人身份接取任务，待接取变为已接取。"
            + "仅在用户已要求处理该任务时执行；尚未加入企划时自动加入，须向用户说明 autoJoinedProject。"
            + "已经由本人接取时直接读取任务继续，不重复接取。",
            annotations = @McpTool.McpAnnotations(destructiveHint = false, openWorldHint = false))
    public String claimTask(McpSyncServerExchange exchange,
            @McpToolParam(description = "任务实例 ID") String instanceId) {
        SessionContext session = userSession.require(exchange);
        return JSON.toJSONString(taskInstanceService.claimTask(TaskInstanceOperateParamsBO.builder()
                .instanceId(McpUserSession.positiveId(instanceId)).userId(session.userId()).orgId(session.orgId()).build()));
    }

    @McpTool(name = "get_task_file_url", description = "为有权读取的材料签发临时下载链接。"
            + "source=task 时 ownerId 为任务实例 ID，fileId 为附件文件 ID；"
            + "source=project 时 ownerId 为企划 ID，fileId 为 references 中的 attachmentId。"
            + "链接过期后重新调用。本工具只取地址，文件下载和处理由客户端完成。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false))
    public String getTaskFileUrl(McpSyncServerExchange exchange,
            @McpToolParam(description = "材料来源：task 或 project") String source,
            @McpToolParam(description = "任务实例 ID 或企划 ID") String ownerId,
            @McpToolParam(description = "task 使用文件 ID，project 使用 attachmentId") String fileId) {
        SessionContext session = userSession.require(exchange);
        Long owner = McpUserSession.positiveId(ownerId);
        Long file = McpUserSession.positiveId(fileId);
        if ("task".equals(source)) {
            return JSON.toJSONString(taskInstanceService.getTaskAttachmentDownload(TaskAttachmentDownloadParamsBO.builder()
                    .instanceId(owner).fileId(file).orgId(session.orgId()).build()));
        }
        if ("project".equals(source)) {
            return JSON.toJSONString(projectAttachmentService.getProjectAttachmentDownload(
                    ProjectAttachmentDownloadParamsBO.builder().projectId(owner).attachmentId(file).orgId(session.orgId()).build()));
        }
        throw new AppException(ResultCode.PARAM_ERROR, "source 必须为 task 或 project");
    }

    @McpTool(name = "create_task_upload", description = "为任务产出文件创建上传地址，固定为 task_submit。"
            + "客户端立即 PUT 原始字节到 uploadUrl，Content-Length 等于 fileSize，只使用返回的 headers，"
            + "不要额外加 Content-Type。上传成功后才能将 fileId 保存到暂存；链接过期则重新创建上传。",
            annotations = @McpTool.McpAnnotations(destructiveHint = false))
    public String createTaskUpload(McpSyncServerExchange exchange,
            @McpToolParam(description = "原始文件名，最长 255 字符") String fileName,
            @McpToolParam(description = "扩展名，不带点，使用现有文件上传允许的类型") String fileExt,
            @McpToolParam(description = "文件大小，单位字节；压缩包最多 500MB，其他文件最多 50MB") Long fileSize,
            @McpToolParam(description = "文件 MIME 类型") String fileMime) {
        SessionContext session = userSession.require(exchange);
        CreateFileReqDTO dto = new CreateFileReqDTO(fileName, fileExt, fileSize, fileMime, "task_submit");
        validate(dto);
        var result = resourceService.create(new ResourceCreateParamsBO(session.userId(), session.orgId(),
                fileName, fileExt, fileSize, fileMime, "task_submit"));
        Map<String, String> headers = result.getContentDisposition() == null ? Map.of()
                : Map.of("Content-Disposition", result.getContentDisposition());
        return JSON.toJSONString(new McpTaskQueryResultBO.Upload(result.getFileId().toString(), "PUT", result.getUploadUrl(), headers));
    }

    @McpTool(name = "save_task_draft", description = "覆盖当前用户已接取任务的暂存。values 为完整字段值，"
            + "省略的可选字段会清空；修改前先读取并保留未改字段。可以暂存未完成的表单。"
            + "只保存草稿，不正式提交、不解锁后续阶段。成功后告知用户在网页审核、提交，或提出修改意见。",
            annotations = @McpTool.McpAnnotations(destructiveHint = true, idempotentHint = true, openWorldHint = false))
    public String saveTaskDraft(McpSyncServerExchange exchange,
            @McpToolParam(description = "任务实例 ID") String instanceId,
            @McpToolParam(description = "按 submitFields 的 key 填写完整草稿值；文件是 {fileId:字符串}") Map<String, Object> values) {
        SessionContext session = userSession.require(exchange);
        TaskSubmitDTO dto = new TaskSubmitDTO();
        dto.setValues(values);
        validate(dto);
        Long id = taskInstanceService.saveTaskDraft(TaskInstanceSubmitParamsBO.builder()
                .instanceId(McpUserSession.positiveId(instanceId)).userId(session.userId()).orgId(session.orgId()).values(values).build());
        return JSON.toJSONString(Map.of("instanceId", id.toString(), "saved", true,
                "message", "已暂存，请在网页审核后正式提交；需要修改时可继续让 AI 修改并暂存"));
    }

    private McpTaskQueryParamsBO query(McpSyncServerExchange exchange, String projectId, String itemId, String instanceId) {
        SessionContext session = userSession.require(exchange);
        return new McpTaskQueryParamsBO(session.userId(), session.orgId(), McpUserSession.positiveId(projectId),
                McpUserSession.positiveId(itemId), instanceId == null ? null : McpUserSession.positiveId(instanceId));
    }

    private PageParamsBO page(Integer pageNum, Integer pageSize) {
        int number = pageNum == null ? 1 : pageNum;
        int size = pageSize == null ? 20 : pageSize;
        if (number < 1 || size < 1 || size > 100) {
            throw new AppException(ResultCode.PARAM_ERROR, "页码须大于 0，每页数量须为 1 到 100");
        }
        return new PageParamsBO(number, size);
    }

    private <T> void validate(T dto) {
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, violations.stream().map(value -> value.getMessage())
                    .sorted().collect(Collectors.joining("；")));
        }
    }
}
