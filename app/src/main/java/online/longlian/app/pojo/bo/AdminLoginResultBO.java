package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResultBO {
    private Long adminId;
    private String username;
    private String role;
    private String token;
}
