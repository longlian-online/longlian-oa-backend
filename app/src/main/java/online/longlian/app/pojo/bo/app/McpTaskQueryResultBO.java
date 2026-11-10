package online.longlian.app.pojo.bo.app;

import online.longlian.app.pojo.bo.common.TaskFormField;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;

import java.util.List;
import java.util.Map;

public class McpTaskQueryResultBO {

    public record Page<T>(List<T> list, Long total) { }

    public record Project(String projectId, String title, String alias, String description) { }

    public record Item(String itemId, String title, Integer progressPercent, String currentNodeName) { }

    public record Upload(String fileId, String method, String uploadUrl, Map<String, String> headers) { }

    public record Task(String instanceId, String name, Integer sort, Integer parallelSort, String status, String revision,
                       String assigneeId, String assigneeName, boolean mine, boolean hasDraft,
                       boolean canClaim, boolean canWork, List<String> blockedBy) { }

    public record TaskList(String projectId, String projectTitle, String itemId, String itemTitle,
                           String flowDescription, List<Task> tasks) { }

    public record Attachment(String attachmentId, String fileId, String fileName, String fileMime, Long fileSize) { }

    public record PreviousSubmission(String instanceId, String name, TaskInstanceDetailVO.Submission submission) { }

    public record TaskContext(TaskList flow, Task task, List<TaskFormField> submitFields,
                              String projectDescription, String projectMetadata, List<Attachment> references,
                              List<PreviousSubmission> previousSubmissions, Map<String, Object> draftValues,
                              TaskInstanceDetailVO.Draft draft, TaskInstanceDetailVO.Submission submission) { }
}
