package online.longlian.app.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignedUpload {

    private String uploadUrl;
    private String key;
}
