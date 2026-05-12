package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgMemberBaseTaskSubmitCountResultBO {
    private Long memberId;
    private Long userId;
    private Integer totalSubmitCount;
    private List<OrgMemberBaseTaskSubmitCountItemBO> items;
}
