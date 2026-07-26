package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString(exclude = "content")
@NoArgsConstructor
@AllArgsConstructor
public class LocalFileUploadParamsBO {
    private String storageKey;
    private byte[] content;
    private Long userId;
    private Long orgId;
}
