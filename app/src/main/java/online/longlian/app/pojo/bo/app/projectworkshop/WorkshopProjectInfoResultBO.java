package online.longlian.app.pojo.bo.app.projectworkshop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopProjectInfoResultBO {

    private Long id;

    private String title;

    private String coverUrl;

    private String thumbnailUrl;

    private String creatorAvatarUrl;

    private String lastSubmitterUsername;

    private LocalDateTime lastSubmitterAt;
}
