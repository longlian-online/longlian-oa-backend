package online.longlian.app.pojo.vo.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import com.alibaba.fastjson2.annotation.JSONType;
import com.alibaba.fastjson2.JSONWriter;

@Data
@AllArgsConstructor
@JSONType(serializeFeatures = JSONWriter.Feature.WriteNulls)
public class NotificationContentVO {
    private String label;
    private String message;
    private String targetPath;
    private String thumbnailUrl;
}
