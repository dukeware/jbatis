package org.ibatis.persist.impl.predicate;

import java.util.Collections;
import java.util.List;
import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.RenderingContext;

public abstract class AbstractSimplePredicate
		extends AbstractPredicateImpl {
	private static final List<Expression<Boolean>> NO_EXPRESSIONS = Collections.emptyList();

	public AbstractSimplePredicate(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder );
	}

	@Override
	public boolean isJunction() {
		return false;
	}

	@Override
	public BooleanOperator getOperator() {
		return BooleanOperator.AND;
	}

	@Override
	public final List<Expression<Boolean>> getExpressions() {
		return NO_EXPRESSIONS;
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
