package online.longlian.generator.internal;

import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import com.baomidou.mybatisplus.generator.type.ITypeConvertHandler;
import com.baomidou.mybatisplus.generator.type.TypeRegistry;

import static online.longlian.generator.CodeGenerator.getEnumTypeConvertMap;

public class TypeConverter implements ITypeConvertHandler {

    @Override
    public IColumnType convert(GlobalConfig globalConfig, TypeRegistry typeRegistry, TableField.MetaInfo metaInfo) {

        if (metaInfo.getTypeName().equals("TINYINT")) {
            var key = new EnumFieldMeta(metaInfo.getTableName(), metaInfo.getColumnName());
            var enumTypeConvertMap = getEnumTypeConvertMap();

            if (enumTypeConvertMap.containsKey(key)) {
                var value = enumTypeConvertMap.get(key);
                return new ModelEnumMeta(value.getType(), value.getPkg());
            } else {
                System.out.printf("表{%s}字段{%s}为TINYINT类型，但是不存在对应的枚举类型，请核对是否无误!\n", metaInfo.getTableName(), metaInfo.getColumnName());
            }
        }

        return typeRegistry.getColumnType(metaInfo);
    }
}