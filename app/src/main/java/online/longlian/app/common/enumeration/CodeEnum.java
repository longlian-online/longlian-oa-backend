package online.longlian.app.common.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;

public interface CodeEnum extends IEnum<Integer> {
    Integer getCode();

    @Override
    default Integer getValue() {
        return getCode();
    }
}
