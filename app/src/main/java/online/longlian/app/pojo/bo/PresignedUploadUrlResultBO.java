package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class PresignedUploadUrlResultBO {

    private String uploadUrl;
    private String key;
}
