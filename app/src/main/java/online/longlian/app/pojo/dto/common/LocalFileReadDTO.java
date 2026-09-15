package online.longlian.app.pojo.dto.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LocalFileReadDTO {
    @NotBlank
    private String key;
    @NotNull
    private Long expires;
    @NotBlank
    @Pattern(regexp = "[0-9a-f]{64}")
    private String signature;
}
