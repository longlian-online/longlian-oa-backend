package online.longlian.app.common.handler;

import online.longlian.app.common.enumeration.CodeEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(CodeEnum.class)
public class CodeEnumTypeHandler<E extends CodeEnum> extends BaseTypeHandler<E> {

    private final Class<E> enumClass;

    public CodeEnumTypeHandler(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    // 写入数据库：枚举 → 数字
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    // 从数据库读取：数字 → 枚举
    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : getEnum(code);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : getEnum(code);
    }

    @Override
    public E getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : getEnum(code);
    }

    private E getEnum(int code) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        throw new RuntimeException("不支持的枚举值：" + code);
    }
}