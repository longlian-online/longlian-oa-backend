package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResourceReadUrlGetResultBO {
    private String url;
    private Long organizationId;
    private String key;
}
