package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class PresignedUploadUrlResultBO {

    private String uploadUrl;
    private String key;

    /** 服务端写入签名的 disposition 值，原样回传给前端，保证客户端 PUT 头与签名逐字一致。 */
    private String contentDisposition;
}
