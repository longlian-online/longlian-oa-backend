package online.longlian.app.common.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InboxMessageType {

    PROJECT_UPDATE("PROJECT_UPDATE", "项目更新");

    @EnumValue
    private final String code;
    private final String description;
}
