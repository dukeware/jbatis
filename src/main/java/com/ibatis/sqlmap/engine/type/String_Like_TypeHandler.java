package com.ibatis.sqlmap.engine.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class String_Like_TypeHandler extends StringTypeHandler implements ValueHandler<String> {
    public static final TypeHandler<String> INSTANCE = new String_Like_TypeHandler();

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, String jdbcType) throws SQLException {
        if (parameter != null && parameter.length() > 0) {
            ps.setString(i, "%" + parameter + "%");
        } else {
            super.setParameter(ps, i, parameter, jdbcType);
        }
    }

    @Override
    public Object setParameterValue(PreparedStatement ps, int i, String parameter, String jdbcType)
        throws SQLException {
        if (parameter != null && parameter.length() > 0) {
            String value = "%" + parameter + "%";
            ps.setString(i, value);
            return value;
        } else {
            super.setParameter(ps, i, parameter, jdbcType);
            return parameter;
        }
    }

}
