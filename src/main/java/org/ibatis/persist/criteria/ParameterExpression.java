package org.ibatis.persist.criteria;

import org.ibatis.persist.Parameter;

/**
 * Type of criteria query parameter expressions.
 *
 * @param <T>
 *            the type of the parameter expression
 *
 * @since iBatis Persistence 1.0
 */
public interface ParameterExpression<T> extends Parameter<T>, Expression<T> {
}
