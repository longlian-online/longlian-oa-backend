package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.StorageType;

@Data
@Schema(description = "开放接口创建上传资源响应")
public class OpenFileCreateVO {

    /** 与站内口径一致：fileId 为雪花ID，序列化为字符串避免调用方精度丢失。 */
    @JsonLongIdString
    @Schema(type = "string", description = "文件ID，提交任务时在文件字段传 {\"fileId\":...}")
    private Long fileId;

    @Schema(description = "预签名上传地址，向存储服务直接 PUT 文件内容，不走密钥")
    private String uploadUrl;

    @Schema(description = "存储类型：1-本地存储，2-阿里云OSS，3-腾讯云COS")
    private StorageType storageType;

    @Schema(description = "直传需携带的 Content-Disposition 头值；null 表示无需携带")
    private String contentDisposition;
}
