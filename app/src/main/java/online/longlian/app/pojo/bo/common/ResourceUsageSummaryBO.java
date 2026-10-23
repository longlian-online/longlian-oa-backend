package online.longlian.app.pojo.bo.common;

import lombok.Data;

@Data
public class ResourceUsageSummaryBO {
    private Long orgId;
    private String bizType;
    private String state;
    private Long fileCount;
    private Long bytes;
}
