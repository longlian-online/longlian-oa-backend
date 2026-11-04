package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAttachmentResultBO {
    private Long id;
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String fileMime;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime createdAt;
}
