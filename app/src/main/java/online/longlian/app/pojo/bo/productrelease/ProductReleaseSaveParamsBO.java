package online.longlian.app.pojo.bo.productrelease;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReleaseSaveParamsBO {
    private String version;
    private String frontendVersion;
    private String backendVersion;
    private String title;
    private String summary;
    private String content;
    private boolean publish;
}
