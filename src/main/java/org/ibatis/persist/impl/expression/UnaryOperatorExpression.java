package org.ibatis.persist.impl.expression;

import org.ibatis.persist.criteria.Expression;

/**
 * Contract for operators with a single operand.
 */
public interface UnaryOperatorExpression<T> extends Expression<T> {
	/**
	 * Get the operand.
	 *
	 * @return The operand.
	 */
	public Expression<?> getOperand();
}
