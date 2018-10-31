package org.ibatis.persist.impl.predicate;

import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Predicate;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Defines a {@link Predicate} used to wrap an {@link Expression Expression&lt;Boolean&gt;}.
 */
public class BooleanExpressionPredicate
		extends AbstractSimplePredicate {
	private final Expression<Boolean> expression;

	public BooleanExpressionPredicate(CriteriaBuilderImpl criteriaBuilder, Expression<Boolean> expression) {
		super( criteriaBuilder );
		this.expression = expression;
	}

	/**
	 * Get the boolean expression defining the predicate.
	 * 
	 * @return The underlying boolean expression.
	 */
	public Expression<Boolean> getExpression() {
		return expression;
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        ((Renderable) getExpression()).render(rc);
    }
}
