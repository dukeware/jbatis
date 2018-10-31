package org.ibatis.persist.impl.predicate;

import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.expression.UnaryOperatorExpression;

/**
 * Defines a {@link org.ibatis.persist.criteria.Predicate} for checking the
 * nullness state of an expression, aka an <tt>IS [NOT] NULL</tt> predicate.
 * <p/>
 * The <tt>NOT NULL</tt> form can be built by calling the constructor and then
 * calling {@link #not}.
 */
public class NullnessPredicate
		extends AbstractSimplePredicate
		implements UnaryOperatorExpression<Boolean> {
	private final Expression<?> operand;

	/**
	 * Constructs the affirmitive form of nullness checking (<i>IS NULL</i>).  To
	 * construct the negative form (<i>IS NOT NULL</i>) call {@link #not} on the
	 * constructed instance.
	 *
	 * @param criteriaBuilder The query builder from whcih this originates.
	 * @param operand The expression to check.
	 */
	public NullnessPredicate(CriteriaBuilderImpl criteriaBuilder, Expression<?> operand) {
		super( criteriaBuilder );
		this.operand = operand;
	}

	@Override
	public Expression<?> getOperand() {
		return operand;
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        ((Renderable) operand).render(rc);
        rc.append(check(isNegated));
    }

	private String check(boolean negated) {
		return negated ? " is not null" : " is null";
	}
}
