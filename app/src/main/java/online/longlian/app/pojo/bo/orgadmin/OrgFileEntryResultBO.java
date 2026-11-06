package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;
import online.longlian.common.enumeration.OrgFileEntryKind;

import java.time.LocalDateTime;

@Data
@Builder
public class OrgFileEntryResultBO {
    private String id;
    private OrgFileEntryKind kind;
    private String name;
    private Long taskInstanceId;
    private String taskName;
    private String fieldLabel;
    private Long fileId;
    private Long fileSize;
    private String fileMime;
    private String availability;
    private String submitterName;
    private LocalDateTime submittedAt;
}
