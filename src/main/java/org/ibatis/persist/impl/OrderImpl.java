package org.ibatis.persist.impl;

import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Order;

/**
 * Represents an <tt>ORDER BY</tt> fragment.
 */
public class OrderImpl implements Order {
    private final Expression<?> expression;
    private boolean ascending;

    public OrderImpl(Expression<?> expression) {
        this(expression, true);
    }

    public OrderImpl(Expression<?> expression, boolean ascending) {
        this.expression = expression;
        this.ascending = ascending;
    }

    public Order reverse() {
        ascending = !ascending;
        return this;
    }

    public boolean isAscending() {
        return ascending;
    }

    public Expression<?> getExpression() {
        return expression;
    }
}
