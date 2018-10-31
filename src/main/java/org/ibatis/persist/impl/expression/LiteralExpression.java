package org.ibatis.persist.impl.expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.ParameterInfo;
import org.ibatis.persist.impl.RenderingContext;

import com.ibatis.sqlmap.engine.type.TypeHandler;
import com.ibatis.sqlmap.engine.type.TypeHandlerFactory;

/**
 * Represents a literal expression.
 */
public class LiteralExpression<T> extends ExpressionImpl<T> implements ParameterInfo<T> {
	private T literal;

	@SuppressWarnings({ "unchecked" })
	public LiteralExpression(CriteriaBuilderImpl criteriaBuilder, T literal) {
		this( criteriaBuilder, (Class<T>) determineClass( literal ), literal );
	}

	private static Class<?> determineClass(Object literal) {
		return literal == null ? null : literal.getClass();
	}

	public LiteralExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> type, T literal) {
		super( criteriaBuilder, type );
		this.literal = literal;
	}

	public T getLiteral() {
		return (T) literal;
	}

    public void render(RenderingContext rc) {
	    rc.append(this);
	}

    public void renderProjection(RenderingContext rc) {
        // some drivers/servers do not like parameters in the select clause
        if (literal == null) {
            rc.append("null");
        } else if (TypeHandlerFactory.isCharacter(literal)) {
            rc.append('\'' + literal.toString() + '\'');
        } else {
            rc.append(literal.toString());
        }
        if (getAlias() != null) {
            rc.append(" AS ").append(getAlias());
        }
    }

	@Override
	@SuppressWarnings({ "unchecked" })
	public void setJavaType(Class targetType) {
		super.setJavaType( targetType );
		TypeHandler valueHandler = getValueHandler();
		if ( valueHandler == null ) {
			valueHandler = criteriaBuilder().getEntityManager().getDelegate().getTypeHandlerFactory().getTypeHandler( targetType );
			forceConversion( valueHandler );
		}

		if ( valueHandler != null ) {
			literal = (T) valueHandler.valueOf( literal );
		}
	}

    @Override
    public T getParameterValue() {
        return literal;
    }

    @Override
    public void setParameterValue(T value) {
        literal = value;
    }

    @Override
    public Class<T> getParameterType() {
        return getJavaType();
    }

    @Override
    public String toString() {
        return "?" + ":" + literal;
    }

    @Override
    public boolean isBound() {
        return literal != ParameterInfo.None;
    }
}
