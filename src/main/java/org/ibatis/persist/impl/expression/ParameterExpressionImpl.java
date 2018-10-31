package org.ibatis.persist.impl.expression;

import org.ibatis.persist.criteria.ParameterExpression;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.ParameterInfo;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Defines a parameter specification, or the information about a parameter (where it occurs, what is
 * its type, etc).
 */
@SuppressWarnings("unchecked")
public class ParameterExpressionImpl<T>
		extends ExpressionImpl<T>
		implements ParameterExpression<T>, ParameterInfo<T> {
	private final String name;
	private final Integer position;
    private T value = (T) None;


	public ParameterExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			String name) {
		super( criteriaBuilder, javaType );
		this.name = name;
		this.position = null;
	}

	public ParameterExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Integer position) {
		super( criteriaBuilder, javaType );
		this.name = null;
		this.position = position;
	}

	public ParameterExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType) {
		super( criteriaBuilder, javaType );
		this.name = null;
		this.position = null;
	}

	public String getName() {
		return name;
	}

	public Integer getPosition() {
		return position;
	}

	public Class<T> getParameterType() {
		return getJavaType();
	}

	public void render(RenderingContext rc) {
	    rc.append(this);
	}

	public void renderProjection(RenderingContext rc) {
        throw new IllegalStateException( "Parameter cannot occur in select clause" );
	}

    @Override
    public T getParameterValue() {
        return value;
    }

    @Override
    public void setParameterValue(T value) {
        this.value = value;
    }

    @Override
    public boolean isBound() {
        return value != ParameterInfo.None;
    }
    
    @Override
    public String toString() {
        return "?" + (name != null ? name : String.valueOf(position)) + ":" + value;
    }
    
}
