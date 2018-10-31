package org.ibatis.persist.impl.predicate;

import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Predicate to assert the explicit value of a boolean expression:<ul>
 * <li>x = true</li>
 * <li>x = false</li>
 * <li>x <> true</li>
 * <li>x <> false</li>
 * </ul>
 */
public class BooleanAssertionPredicate
		extends AbstractSimplePredicate {
	private final Expression<Boolean> expression;
	private final Boolean assertedValue;

	public BooleanAssertionPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<Boolean> expression,
			Boolean assertedValue) {
		super( criteriaBuilder );
		this.expression = expression;
		this.assertedValue = assertedValue;
	}

	public Expression<Boolean> getExpression() {
		return expression;
	}

	public Boolean getAssertedValue() {
		return assertedValue;
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        final String operator = isNegated ? " <> " : " = ";
        final String assertionLiteral = assertedValue ? "true" : "false";

        ((Renderable) expression).render(rc);
        rc.append(operator).append(assertionLiteral);
    }
}
