package org.ibatis.persist.impl.predicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Subquery;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.expression.LiteralExpression;

/**
 * Models an <tt>[NOT] IN</tt> restriction
 */
@SuppressWarnings("unchecked")
public class InPredicate<T>
		extends AbstractSimplePredicate
		implements CriteriaBuilderImpl.In<T> {
	private final Expression<? extends T> expression;
	private final List<Expression<? extends T>> values;

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with an empty list of values.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression) {
		this( criteriaBuilder, expression, new ArrayList<Expression<? extends T>>() );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given list of expression values.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			Expression<? extends T>... values) {
		this( criteriaBuilder, expression, Arrays.asList( values ) );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given list of expression values.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			List<Expression<? extends T>> values) {
		super( criteriaBuilder );
		this.expression = expression;
		this.values = values;
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given given literal value list.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			T... values) {
		this( criteriaBuilder, expression, Arrays.asList( values ) );
	}

	/**
	 * Constructs an <tt>IN</tt> predicate against a given expression with the given literal value list.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param expression The expression.
	 * @param values The value list.
	 */
	public InPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<? extends T> expression,
			Collection<T> values) {
		super( criteriaBuilder );
		this.expression = expression;
		this.values = new ArrayList<Expression<? extends T>>( values.size() );
        for (T value : values) {
            this.values.add(new LiteralExpression<T>(criteriaBuilder, value));
        }
	}

	@Override
	public Expression<T> getExpression() {
		return ( Expression<T> ) expression;
	}

	public Expression<? extends T> getExpressionInternal() {
		return expression;
	}

	public List<Expression<? extends T>> getValues() {
		return values;
	}

	@Override
	public InPredicate<T> value(T value) {
		return value( new LiteralExpression<T>( criteriaBuilder(), value ) );
	}

	@Override
	public InPredicate<T> value(Expression<? extends T> value) {
		values.add( value );
		return this;
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        ((Renderable) getExpression()).render(rc);

        if (isNegated) {
            rc.append(" not");
        }
        rc.append(" in ");

        // subquery expressions are already wrapped in parenthesis, so we only need to
        // render the parenthesis here if the values represent an explicit value list
        boolean isInSubqueryPredicate = getValues().size() == 1 && Subquery.class.isInstance(getValues().get(0));
        if (isInSubqueryPredicate) {
            ((Renderable) getValues().get(0)).render(rc);
        } else {
            rc.append('(');
            String sep = "";
            for (Expression value : getValues()) {
                rc.append(sep);
                ((Renderable) value).render(rc);
                sep = ", ";
            }
            rc.append(')');
        }
    }
}
