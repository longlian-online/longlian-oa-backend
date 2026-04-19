package online.longlian.app.pojo.vo.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.generator.enumeration.StorageType;

@Data
@AllArgsConstructor
@Schema(description = "创建文件上传响应")
public class ResourcCreateVO {

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "预签名上传地址")
    private String uploadUrl;

    @Schema(description = "文件存储唯一标识（storageKey）")
    private String key;

    @Schema(description = "存储类型：1-本地存储，2-阿里云OSS，3-腾讯云COS")
    private StorageType storageType;
}
