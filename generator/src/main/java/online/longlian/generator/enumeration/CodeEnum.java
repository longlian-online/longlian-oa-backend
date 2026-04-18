package online.longlian.generator.enumeration;

import com.baomidou.mybatisplus.annotation.IEnum;

public interface CodeEnum extends IEnum<Integer> {
    Integer getCode();

    @Override
    default Integer getValue() {
        return getCode();
    }
}
