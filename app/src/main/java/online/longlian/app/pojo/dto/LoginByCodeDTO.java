package online.longlian.app.pojo.dto;

import lombok.Data;

@Data
public class LoginByCodeDTO {
    private String email;
    private String code;
}
