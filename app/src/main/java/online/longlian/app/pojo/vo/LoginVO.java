package online.longlian.app.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {
    private String token;
    private List<String> roles;
}
