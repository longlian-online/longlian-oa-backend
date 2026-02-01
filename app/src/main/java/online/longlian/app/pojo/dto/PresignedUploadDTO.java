package online.longlian.app.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignedUploadDTO {

    private String uploadUrl;
    private String key;
}
