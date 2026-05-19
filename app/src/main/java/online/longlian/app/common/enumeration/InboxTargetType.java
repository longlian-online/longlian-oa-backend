package online.longlian.app.common.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InboxTargetType {

    USER(1, "个人"),
    ORGANIZATION(2, "组织");

    @EnumValue
    private final int code;
    private final String description;
}
