package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCreateParamsBO {
    private Long projectId;
    private Long orgId;
    private String title;
    private Long taskTemplateId;
    private Long creatorId;
    /** 不传则按模板原样生成。传入时以这份步骤创建，并可带执行人。 */
    private List<ItemNodeCreateParamsBO> nodes;
}
