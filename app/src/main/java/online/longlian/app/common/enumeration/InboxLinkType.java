package online.longlian.app.common.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InboxLinkType {

    INTERNAL(1, "站内"),
    EXTERNAL(2, "站外");

    @EnumValue
    private final int code;
    private final String description;
}
