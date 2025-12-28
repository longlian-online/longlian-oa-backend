package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignedUpload {

    private String uploadUrl;
    private String key;
}
