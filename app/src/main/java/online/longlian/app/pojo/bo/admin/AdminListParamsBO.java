package online.longlian.app.pojo.bo.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.pojo.bo.common.PageParamsBO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminListParamsBO {
    private PageParamsBO page;
}
