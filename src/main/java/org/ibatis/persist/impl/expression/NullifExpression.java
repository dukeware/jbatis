package org.ibatis.persist.impl.expression;

import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Models an ANSI SQL <tt>NULLIF</tt> expression.  <tt>NULLIF</tt> is a specialized <tt>CASE</tt> statement.
 */
@SuppressWarnings("unchecked")
public class NullifExpression<T> extends ExpressionImpl<T> {
	private final Expression<? extends T> primaryExpression;
	private final Expression<?> secondaryExpression;

	public NullifExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Expression<? extends T> primaryExpression,
			Expression<?> secondaryExpression) {
		super( criteriaBuilder, (Class<T>)determineType(javaType, primaryExpression) );
		this.primaryExpression = primaryExpression;
		this.secondaryExpression = secondaryExpression;
	}

	public NullifExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Expression<? extends T> primaryExpression,
			Object secondaryExpression) {
		super( criteriaBuilder, (Class<T>)determineType(javaType, primaryExpression) );
		this.primaryExpression = primaryExpression;
		this.secondaryExpression = new LiteralExpression( criteriaBuilder, secondaryExpression );
	}

	private static Class determineType(Class javaType, Expression primaryExpression) {
		return javaType != null ? javaType : primaryExpression.getJavaType();
	}

	public Expression<? extends T> getPrimaryExpression() {
		return primaryExpression;
	}

	public Expression<?> getSecondaryExpression() {
		return secondaryExpression;
	}

    public void render(RenderingContext rc) {
        rc.append("nullif(");
        ((Renderable) getPrimaryExpression()).render(rc);
        rc.append(',');
        ((Renderable) getSecondaryExpression()).render(rc);
        rc.append(")");
    }

    public void renderProjection(RenderingContext rc) {
        render(rc);
        if (getAlias() != null) {
            rc.append(" AS ").append(getAlias());
        }
    }
}
