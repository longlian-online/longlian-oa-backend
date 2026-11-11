package online.longlian.app.pojo.bo.app.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.ItemNodeState;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectItemNodeResultBO {

    private String name;

    private Integer sort;

    private Integer parallelSort;

    private ItemNodeState state;

    private Integer parallelCount;
}
