package org.ibatis.persist.impl.predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Predicate;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * A compound {@link Predicate predicate} is a grouping of other {@link Predicate predicates} in order to convert
 * either a conjunction (logical AND) or a disjunction (logical OR).
 */
@SuppressWarnings("unchecked")
public class CompoundPredicate
		extends AbstractPredicateImpl {
	private BooleanOperator operator;
	private final List<Expression<Boolean>> expressions = new ArrayList<Expression<Boolean>>();

	/**
	 * Constructs an empty conjunction or disjunction.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param operator Indicates whether this predicate will function
	 * as a conjunction or disjunction.
	 */
	public CompoundPredicate(CriteriaBuilderImpl criteriaBuilder, BooleanOperator operator) {
		super( criteriaBuilder );
		this.operator = operator;
	}

	/**
	 * Constructs a conjunction or disjunction over the given expressions.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param operator Indicates whether this predicate will function
	 * as a conjunction or disjunction.
	 * @param expressions The expressions to be grouped.
	 */
	public CompoundPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			BooleanOperator operator,
			Expression<Boolean>... expressions) {
		this( criteriaBuilder, operator );
		applyExpressions( expressions );
	}

	/**
	 * Constructs a conjunction or disjunction over the given expressions.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param operator Indicates whether this predicate will function
	 * as a conjunction or disjunction.
	 * @param expressions The expressions to be grouped.
	 */
	public CompoundPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			BooleanOperator operator,
			List<Expression<Boolean>> expressions) {
		this( criteriaBuilder, operator );
		applyExpressions( expressions );
	}

	private void applyExpressions(Expression<Boolean>... expressions) {
		applyExpressions( Arrays.asList( expressions ) );
	}

	private void applyExpressions(List<Expression<Boolean>> expressions) {
		this.expressions.clear();
		this.expressions.addAll( expressions );
	}

	@Override
	public BooleanOperator getOperator() {
		return operator;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return expressions;
	}

	@Override
    public void render(RenderingContext rc) {
        render(isNegated(), rc);
    }

	@Override
	public boolean isJunction() {
		return true;
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        render(this, rc);
    }

	@Override
    public void renderProjection(RenderingContext rc) {
        render(rc);
    }

	/**
	 * Create negation of compound predicate by using logic rules:
	 * 1. not (x || y) is (not x && not y)
	 * 2. not (x && y) is (not x || not y)
	 */
	@Override
	public Predicate not() {
		return new NegatedPredicateWrapper( this );
	}

	public static BooleanOperator reverseOperator(BooleanOperator operator) {
		return operator == BooleanOperator.AND
				? BooleanOperator.OR
				: BooleanOperator.AND;
	}

    public static void render(PredicateImplementor predicate, RenderingContext rc) {
        if (!predicate.isJunction()) {
            throw new IllegalStateException("CompoundPredicate.render should only be used to render junctions");
        }

        // for junctions, the negation is already cooked into the expressions and operator; we just need to render
        // them as is

        if (predicate.getExpressions().isEmpty()) {
            boolean implicitTrue = predicate.getOperator() == BooleanOperator.AND;
            // AND is always true for empty; OR is always false
            rc.append(implicitTrue ? "1=1" : "0=1");
            return;
        }

        // single valued junction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        if (predicate.getExpressions().size() == 1) {
            ((Renderable) predicate.getExpressions().get(0)).render(rc);
            return;
        }

        String sep = "";
        for (Expression expression : predicate.getExpressions()) {
            rc.append(sep).append("( ");
            ((Renderable) expression).render(rc);
            rc.append(" )");
            sep = operatorTextWithSeparator(predicate.getOperator());
        }
    }

	private static String operatorTextWithSeparator(BooleanOperator operator) {
		return operator == BooleanOperator.AND
				? " and "
				: " or ";
	}
}
