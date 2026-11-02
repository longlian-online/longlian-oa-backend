package online.longlian.app.pojo.vo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductReleaseBaselineVO {
    private String frontendVersion;
    private String backendVersion;
}
