package online.longlian.app.pojo.vo.app;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
@Schema(description = "当前产品版本")
public class ProductReleaseCurrentVO {

    @Schema(nullable = true, description = "当前发布版本；没有已发布版本时为 null")
    private ProductReleaseNoticeVO release;

    @Schema(description = "早于当前版本的已发布版本数量")
    private int olderCount;
}
