package online.longlian.app.pojo.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private Integer status;
}
