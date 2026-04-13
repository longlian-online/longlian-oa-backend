package online.longlian.generator.internal;

import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ModelEnumMeta implements IColumnType {
    private final String className;
    private final String packageName;

    @Override
    public String getType() {
        return className;
    }

    @Override
    public String getPkg() {
        return packageName;
    }
}
