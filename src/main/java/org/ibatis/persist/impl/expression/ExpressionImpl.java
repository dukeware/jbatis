package org.ibatis.persist.impl.expression;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.ExpressionImplementor;

/**
 * Models an expression in the criteria query language.
 */
@SuppressWarnings("unchecked")
public abstract class ExpressionImpl<T>
		extends SelectionImpl<T>
		implements ExpressionImplementor<T> {
	public ExpressionImpl(CriteriaBuilderImpl criteriaBuilder, Class<T> javaType) {
		super( criteriaBuilder, javaType );
	}

	@Override
    public <X> Expression<X> as(Class<X> type) {
	    ExpressionImpl<X> x = (ExpressionImpl<X>) this;
        x.setJavaType(type);
        return x;
    }

	@Override
	public Predicate isNull() {
		return criteriaBuilder().isNull( this );
	}

	@Override
	public Predicate isNotNull() {
		return criteriaBuilder().isNotNull( this );
	}

	@Override
    public Predicate in(Object... values) {
		return criteriaBuilder().in( this, values );
	}

	@Override
	public Predicate in(Expression<?>... values) {
		return criteriaBuilder().in( this, (Expression[]) values );
	}

	@Override
	public Predicate in(Collection<?> values) {
		return criteriaBuilder().in( this, values.toArray() );
	}

	@Override
	public Predicate in(Expression<Collection<?>> values) {
		return criteriaBuilder().in( this, values );
	}

	@Override
	public ExpressionImplementor<Long> asLong() {
		setJavaType( Long.class );
		return (ExpressionImplementor<Long>) this;
	}

	@Override
	public ExpressionImplementor<Integer> asInteger() {
		setJavaType( Integer.class );
		return (ExpressionImplementor<Integer>) this;
	}

	@Override
	public ExpressionImplementor<Float> asFloat() {
		setJavaType( Float.class );
		return (ExpressionImplementor<Float>) this;
	}

	@Override
	public ExpressionImplementor<Double> asDouble() {
		setJavaType( Double.class );
		return (ExpressionImplementor<Double>) this;
	}

	@Override
	public ExpressionImplementor<BigDecimal> asBigDecimal() {
		setJavaType( BigDecimal.class );
		return (ExpressionImplementor<BigDecimal>) this;
	}

	@Override
	public ExpressionImplementor<BigInteger> asBigInteger() {
		setJavaType( BigInteger.class );
		return (ExpressionImplementor<BigInteger>) this;
	}

	@Override
	public ExpressionImplementor<String> asString() {
		setJavaType( String.class );
		return (ExpressionImplementor<String>) this;
	}
}
