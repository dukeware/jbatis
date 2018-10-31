package org.ibatis.persist.impl.predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.expression.ExpressionImpl;

public class NegatedPredicateWrapper extends ExpressionImpl<Boolean> implements PredicateImplementor {
	private final PredicateImplementor predicate;
	private final BooleanOperator negatedOperator;
	private final List<Expression<Boolean>> negatedExpressions;

	public NegatedPredicateWrapper(PredicateImplementor predicate) {
		super( predicate.criteriaBuilder(), Boolean.class );
		this.predicate = predicate;
		this.negatedOperator = predicate.isJunction()
				? CompoundPredicate.reverseOperator( predicate.getOperator() )
				: predicate.getOperator();
		this.negatedExpressions = negateCompoundExpressions( predicate.getExpressions(), predicate.criteriaBuilder() );
	}

	private static List<Expression<Boolean>> negateCompoundExpressions(
			List<Expression<Boolean>> expressions,
			CriteriaBuilderImpl criteriaBuilder) {
		if ( expressions == null || expressions.isEmpty() ) {
			return Collections.emptyList();
		}

		final List<Expression<Boolean>> negatedExpressions = new ArrayList<Expression<Boolean>>();
		for ( Expression<Boolean> expression : expressions ) {
			if ( Predicate.class.isInstance( expression ) ) {
				negatedExpressions.add( ( (Predicate) expression ).not() );
			}
			else {
				negatedExpressions.add( criteriaBuilder.not( expression ) );
			}
		}
		return negatedExpressions;
	}

	@Override
	public BooleanOperator getOperator() {
		return negatedOperator;
	}

	@Override
	public boolean isJunction() {
		return predicate.isJunction();
	}

	@Override
	public boolean isNegated() {
		return ! predicate.isNegated();
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return negatedExpressions;
	}

	@Override
	public Predicate not() {
		return new NegatedPredicateWrapper( this );
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        if (isJunction()) {
            CompoundPredicate.render(this, rc);
        } else {
            predicate.render(isNegated, rc);
        }
    }

	@Override
    public void render(RenderingContext rc) {
        render(isNegated(), rc);
    }

	@Override
    public void renderProjection(RenderingContext rc) {
        render(rc);
        if (getAlias() != null) {
            rc.append(" AS ").append(getAlias());
        }
    }
}
