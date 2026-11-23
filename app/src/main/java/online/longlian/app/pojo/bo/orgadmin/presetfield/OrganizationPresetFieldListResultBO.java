package online.longlian.app.pojo.bo.orgadmin.presetfield;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationPresetFieldListResultBO {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private String creatorNickname;
}
