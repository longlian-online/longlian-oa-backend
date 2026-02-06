package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignedUploadBO {

    private String uploadUrl;
    private String key;
}
