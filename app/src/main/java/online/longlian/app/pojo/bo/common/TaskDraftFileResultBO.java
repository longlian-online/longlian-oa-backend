package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDraftFileResultBO {

    private String fileId;
    private String fileName;
    private Long fileSize;
    private String fileMime;
    private String availability;
}
