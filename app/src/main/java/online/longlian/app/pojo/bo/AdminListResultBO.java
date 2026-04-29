package online.longlian.app.pojo.bo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminListResultBO {
    private Long id;
    private String username;
    private String role;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
