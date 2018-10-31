package org.ibatis.persist.impl.function;

import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
/**
 * Models the ANSI SQL <tt>SQRT</tt> function.
 */
public class SqrtFunction
		extends ParameterizedFunctionExpression<Double> {
	public static final String NAME = "sqrt";

	public SqrtFunction(CriteriaBuilderImpl criteriaBuilder, Expression<? extends Number> expression) {
		super( criteriaBuilder, Double.class, NAME, expression );
	}

	@Override
	protected boolean isStandardJpaFunction() {
		return true;
	}
}
