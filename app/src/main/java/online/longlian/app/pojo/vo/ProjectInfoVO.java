package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.ProjectStatus;

@Data
@Schema(description = "企划信息")
public class ProjectInfoVO {

    @Schema(description = "企划ID")
    private Long id;

    @Schema(description = "企划标题")
    private String title;

    @Schema(description = "企划简介")
    private String description;

    @Schema(description = "封面图片URL")
    private String coverUrl;

    @Schema(description = "企划类型")
    private String projectType;

    @Schema(description = "企划状态，1-进行中 2-已完成 3-已归档")
    private ProjectStatus projectStatus;

    @Schema(description = "企划作者")
    private String author;

    @Schema(description = "创建人头像URL")
    private String creatorAvatarUrl;
}