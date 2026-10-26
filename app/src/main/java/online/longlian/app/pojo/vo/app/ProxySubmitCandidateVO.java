package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "可代提交的组员")
public class ProxySubmitCandidateVO {

    @JsonLongIdString
    @Schema(type = "string", description = "组员用户ID")
    private Long userId;

    @Schema(description = "组员昵称")
    private String nickname;
}
