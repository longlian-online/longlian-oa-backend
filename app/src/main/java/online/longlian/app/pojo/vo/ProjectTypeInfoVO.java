package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Schema(description = "企划类型信息")
public class ProjectTypeInfoVO {

    @Schema(description = "企划类型ID")
    private Long id;

    @Schema(description = "类型名称（如：漫画/小说/美术/视频）")
    private String name;

}
