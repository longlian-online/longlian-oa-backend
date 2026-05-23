package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectProgressBO {
    private Integer progressPercent;
    private Integer claimedTaskCount;
    private Integer pendingTaskCount;
}
