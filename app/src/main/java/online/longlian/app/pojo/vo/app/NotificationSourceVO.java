package online.longlian.app.pojo.vo.app;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@AllArgsConstructor
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
@Schema(description = "通知来源基础数据")
public class NotificationSourceVO {

    @JsonLongIdString
    @Schema(type = "string", description = "企划ID")
    private Long projectId;

    @Schema(description = "企划名称，无企划来源时为 null")
    private String projectTitle;

    @JsonLongIdString
    @Schema(type = "string", description = "项目ID")
    private Long itemId;

    @Schema(description = "项目名称，非项目来源时为 null")
    private String itemTitle;

    @Schema(nullable = true, description = "任务来源的当前槽位结构；项目完成通知为 null")
    private NotificationTaskSlotVO taskSlot;

    @Schema(nullable = true, description = "入组申请人显示名")
    private String applicantName;

    @Schema(nullable = true, description = "删除申请人显示名")
    private String requesterName;

    @Schema(nullable = true, allowableValues = {"PROJECT", "ITEM"}, description = "删除目标类型")
    private String targetType;

    @Schema(nullable = true, description = "删除目标提交时的名称")
    private String targetTitle;

    @JsonLongIdString
    @Schema(type = "string", nullable = true, description = "打回通知关联的任务实例ID；其他类型为 null")
    private Long taskInstanceId;

    @Schema(nullable = true, description = "打回意见；其他类型为 null")
    private String reviewComment;
}
