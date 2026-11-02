package online.longlian.app.pojo.bo.productrelease;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductReleaseBaselineResultBO {
    private String frontendVersion;
    private String backendVersion;
}
