package online.longlian.app.pojo.vo.app;

import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.JSONWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
public class ProductReleaseCurrentVO {
    private ProductReleaseNoticeVO release;
    private int olderCount;
}
