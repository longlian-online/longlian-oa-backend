package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfoResultBO {

    private String version;

    private String buildTime;

    private String displayName;
}
