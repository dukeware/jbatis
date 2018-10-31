package org.ibatis.persist.impl.expression;

import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * A string concatenation.
 * TODO CONCAT(x, y)
 */
public class ConcatExpression extends ExpressionImpl<String> {
	private Expression<String> string1;
	private Expression<String> string2;

	public ConcatExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> expression1,
			Expression<String> expression2) {
		super( criteriaBuilder, String.class );
		this.string1 = expression1;
		this.string2 = expression2;
	}

	public ConcatExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> string1, 
			String string2) {
		this( criteriaBuilder, string1, wrap( criteriaBuilder, string2) );
	}

	private static Expression<String> wrap(CriteriaBuilderImpl criteriaBuilder, String string) {
		return new LiteralExpression<String>( criteriaBuilder, string );
	}

	public ConcatExpression(
			CriteriaBuilderImpl criteriaBuilder,
			String string1,
			Expression<String> string2) {
		this( criteriaBuilder, wrap( criteriaBuilder, string1), string2 );
	}

	public Expression<String> getString1() {
		return string1;
	}

	public Expression<String> getString2() {
		return string2;
	}

    public void render(RenderingContext rc) {
        ((Renderable) getString1()).render(rc);
        rc.append(" || ");
        ((Renderable) getString2()).render(rc);
    }

	public void renderProjection(RenderingContext rc) {
        render(rc);
        if (getAlias() != null) {
            rc.append(" AS ").append(getAlias());
        }
	}
}
