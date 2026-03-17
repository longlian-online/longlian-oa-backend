package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.enumeration.StorageType;

@Data
@AllArgsConstructor
@Schema(description = "创建文件上传响应")
public class CreateFileResVO {

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "预签名上传地址")
    private String uploadUrl;

    @Schema(description = "文件存储唯一标识")
    private String key;

    @Schema(description = "存储类型 1-本地 2-OSS 3-COS")
    private StorageType storageType;
}