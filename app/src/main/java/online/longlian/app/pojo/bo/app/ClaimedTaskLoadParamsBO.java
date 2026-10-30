package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClaimedTaskLoadParamsBO {
    private Long orgId;
    private List<Long> assigneeIds;
}
