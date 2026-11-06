package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "已暂存的任务成果")
public class TaskDraftContentVO {

    @Schema(description = "草稿ID")
    private String id;

    @Schema(description = "本次暂存修订号")
    private String revision;

    @Schema(description = "最近暂存时间")
    private LocalDateTime updatedAt;

    @Schema(description = "已暂存的表单值")
    private Map<String, Object> values;

    @Schema(description = "按文件ID索引的附件信息")
    private Map<String, TaskDraftFileVO> files;
}
