package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PresignedUploadUrlParamsBO {
    private String key;
    /** 上传时需写入对象元数据的 Content-Disposition 头值；与业务无关（头像/封面/本地存储）时为 null。 */
    private String contentDisposition;
}
