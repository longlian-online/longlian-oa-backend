package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionRefreshResultBO {
    private String token;
    private String refreshToken;
}
