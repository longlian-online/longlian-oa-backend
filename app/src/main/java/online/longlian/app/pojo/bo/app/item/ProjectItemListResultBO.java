package online.longlian.app.pojo.bo.app.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import online.longlian.common.enumeration.ItemStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectItemListResultBO {

    private Long id;

    private String title;

    private ItemStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer progressPercent;

    private String currentNodeName;

    private Boolean hasSubmissionRecord;

    private List<ProjectItemNodeResultBO> nodes;
}
