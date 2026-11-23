package online.longlian.app.pojo.bo.app.taskinstance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务提交附件的展示信息。读取链接只在点击预览或下载时另行签发。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAttachmentResultBO {
    private String id;
    private String name;
    private String sizeText;
    private String mediaType;
    private String availability;
}
