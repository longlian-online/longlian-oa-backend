package online.longlian.app.pojo.bo.app.taskplaza;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlazaProjectGroupResultBO {

    private Long projectId;

    private String title;

    private String typeName;

    private String coverUrl;

    private String thumbnailUrl;

    private Integer pendingCount;

    private Integer itemCount;

    private List<PlazaTaskResultBO> tasks;
}
