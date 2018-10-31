package org.ibatis.persist.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import org.ibatis.persist.criteria.Expression;

/**
 * Internal contract for implementations of the JPA {@link Expression} contract.
 */
public interface ExpressionImplementor<T> extends SelectionImplementor<T>, Expression<T> {
	/**
	 * See {@link org.ibatis.persist.criteria.CriteriaBuilder#toLong}
	 *
	 * @return <tt>this</tt> but as a long
	 */
	public ExpressionImplementor<Long> asLong();

	/**
	 * See {@link org.ibatis.persist.criteria.CriteriaBuilder#toInteger}
	 *
	 * @return <tt>this</tt> but as an integer
	 */
	public ExpressionImplementor<Integer> asInteger();

	/**
	 * See {@link org.ibatis.persist.criteria.CriteriaBuilder#toFloat}
	 *
	 * @return <tt>this</tt> but as a float
	 */
	public ExpressionImplementor<Float> asFloat();

	/**
	 * See {@link org.ibatis.persist.criteria.CriteriaBuilder#toDouble}
	 *
	 * @return <tt>this</tt> but as a double
	 */
	public ExpressionImplementor<Double> asDouble();

	/**
	 * See {@link org.ibatis.persist.criteria.CriteriaBuilder#toBigDecimal}
	 *
	 * @return <tt>this</tt> but as a {@link BigDecimal}
	 */
	public ExpressionImplementor<BigDecimal> asBigDecimal();

	/**
	 * See {@link org.ibatis.persist.criteria.CriteriaBuilder#toBigInteger}
	 *
	 * @return <tt>this</tt> but as a {@link BigInteger}
	 */
	public ExpressionImplementor<BigInteger> asBigInteger();

	/**
	 * See {@link org.ibatis.persist.criteria.CriteriaBuilder#toString}
	 *
	 * @return <tt>this</tt> but as a string
	 */
	public ExpressionImplementor<String> asString();

}
