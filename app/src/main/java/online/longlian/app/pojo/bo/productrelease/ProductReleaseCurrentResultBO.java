package online.longlian.app.pojo.bo.productrelease;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductReleaseCurrentResultBO {
    private ProductReleaseNoticeResultBO release;
    private int olderCount;

    public static ProductReleaseCurrentResultBO none() {
        return new ProductReleaseCurrentResultBO(null, 0);
    }
}
