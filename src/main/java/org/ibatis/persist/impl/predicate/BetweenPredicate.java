package org.ibatis.persist.impl.predicate;

import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Models a <tt>BETWEEN</tt> {@link org.ibatis.persist.criteria.Predicate}.
 */
public class BetweenPredicate<Y>
		extends AbstractSimplePredicate {
	private final Expression<? extends Y> expression;
	private final Expression<? extends Y> lowerBound;
	private final Expression<? extends Y> upperBound;

	public BetweenPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends Y> expression,
			Y lowerBound,
			Y upperBound) {
		this(
				criteriaBuilder,
				expression,
				criteriaBuilder.literal( lowerBound ),
				criteriaBuilder.literal( upperBound )
		);
	}

	public BetweenPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends Y> expression,
			Expression<? extends Y> lowerBound,
			Expression<? extends Y> upperBound) {
		super( criteriaBuilder );
		this.expression = expression;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public Expression<? extends Y> getExpression() {
		return expression;
	}

	public Expression<? extends Y> getLowerBound() {
		return lowerBound;
	}

	public Expression<? extends Y> getUpperBound() {
		return upperBound;
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        final String operator = isNegated ? " not between " : " between ";
        ((Renderable) getExpression()).render(rc);
        rc.append(operator);
        ((Renderable) getLowerBound()).render(rc);
        rc.append(" and ");
        ((Renderable) getUpperBound()).render(rc);
    }
}
