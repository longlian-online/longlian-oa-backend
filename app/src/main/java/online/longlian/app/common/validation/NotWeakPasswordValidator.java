package online.longlian.app.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.annotation.NotWeakPassword;

@RequiredArgsConstructor
public class NotWeakPasswordValidator implements ConstraintValidator<NotWeakPassword, String> {

    private final WeakPasswordDictionary dictionary;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 空值由 DTO 的 @NotBlank 提供统一提示。
        return value == null || !dictionary.contains(value);
    }
}
