package org.ibatis.persist.impl.expression;

import java.util.ArrayList;
import java.util.List;
import org.ibatis.persist.criteria.CriteriaBuilder.Coalesce;
import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Models an ANSI SQL <tt>COALESCE</tt> expression.  <tt>COALESCE</tt> is a specialized <tt>CASE</tt> statement.
 */
@SuppressWarnings("unchecked")
public class CoalesceExpression<T> extends ExpressionImpl<T> implements Coalesce<T> {
	private final List<Expression<? extends T>> expressions;
	private Class<T> javaType;

	public CoalesceExpression(CriteriaBuilderImpl criteriaBuilder) {
		this( criteriaBuilder, null );
	}

	public CoalesceExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType) {
		super( criteriaBuilder, javaType );
		this.javaType = javaType;
		this.expressions = new ArrayList<Expression<? extends T>>();
	}

	@Override
	public Class<T> getJavaType() {
		return javaType;
	}

	public Coalesce<T> value(T value) {
		return value( new LiteralExpression<T>( criteriaBuilder(), value ) );
	}

	public Coalesce<T> value(Expression<? extends T> value) {
		expressions.add( value );
		if ( javaType == null ) {
			javaType = (Class<T>) value.getJavaType();
		}
		return this;
	}

	public List<Expression<? extends T>> getExpressions() {
		return expressions;
	}

    public void render(RenderingContext rc) {
        rc.append("coalesce(");
        String sep = "";
        for (Expression expression : getExpressions()) {
            rc.append(sep);
            ((Renderable) expression).render(rc);
            sep = ", ";
        }
        rc.append(")");
    }

    public void renderProjection(RenderingContext rc) {
        render(rc);
        if (getAlias() != null) {
            rc.append(" AS ").append(getAlias());
        }
    }
}
