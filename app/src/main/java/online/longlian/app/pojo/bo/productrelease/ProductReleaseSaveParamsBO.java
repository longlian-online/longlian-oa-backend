package online.longlian.app.pojo.bo.productrelease;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private List<String> added;
    private List<String> changed;
    private List<String> fixed;
    private boolean publish;
}
