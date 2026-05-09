package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageParamsBO {
    private Integer pageNum;
    private Integer pageSize;
}
