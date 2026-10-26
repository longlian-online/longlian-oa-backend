package online.longlian.app.pojo.vo.app;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.common.enumeration.ProjectStatus;

import java.time.LocalDateTime;

@Data
@Schema(description = "企划信息")
public class ProjectInfoVO {

    @JsonLongIdString
    @Schema(type = "string", description = "企划ID")
    private Long id;

    @Schema(description = "企划标题")
    private String title;

    @Schema(description = "企划别名")
    private String alias;

    @Schema(description = "企划简介")
    private String description;

    @Schema(description = "封面图片URL")
    private String coverUrl;

    @Schema(description = "企划类型")
    private String projectType;

    @Schema(description = "企划状态，1-进行中 2-已完成 3-已归档")
    private ProjectStatus projectStatus;

    @Schema(description = "扩展信息(JSON对象字符串，包含 tags 数组，每项含 key 及可选的 value、color；color 为 blue、green、amber、rose、violet 之一；搜索结果中命中的 tags 稳定前置并保留完整标签。可选 fields 数组按保存顺序返回，搜索命中不调整 fields。数据库中的顺序不变)")
    private String metadata;

    @Schema(description = "创建人头像URL")
    private String creatorAvatarUrl;

    @Schema(description = "创建人昵称")
    private String creatorNickname;

    @Schema(description = "创建人在当前组织最近一次任务提交时间，打回不回退；无提交记录时为空")
    private LocalDateTime creatorLastSubmittedAt;
}
