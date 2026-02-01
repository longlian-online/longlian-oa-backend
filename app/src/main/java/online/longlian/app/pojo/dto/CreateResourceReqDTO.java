package online.longlian.app.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.enumeration.ResourceType;

@Data
@AllArgsConstructor
public class CreateResourceReqDTO {
    private String ext;
    private ResourceType type;
    private Integer size;
}
