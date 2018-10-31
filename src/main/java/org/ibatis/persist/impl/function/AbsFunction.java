package org.ibatis.persist.impl.function;

import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;

/**
 * Models the ANSI SQL <tt>ABS</tt> function.
 */
@SuppressWarnings("unchecked")
public class AbsFunction<N extends Number>
		extends ParameterizedFunctionExpression<N> {
	public static final String NAME = "abs";

	public AbsFunction(CriteriaBuilderImpl criteriaBuilder, Expression expression) {
		super( criteriaBuilder, expression.getJavaType(), NAME, expression );
	}

	@Override
	protected boolean isStandardJpaFunction() {
		return true;
	}
}
