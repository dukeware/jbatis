package org.ibatis.persist.criteria;

import java.util.List;

/**
 * The <code>Subquery</code> interface defines functionality that is specific to subqueries.
 *
 * A subquery has an expression as its selection item.
 *
 * @param <T>
 *            the type of the selection item.
 *
 * @since iBatis Persistence 1.0
 */
public interface Subquery<T> extends AbstractQuery<T>, Expression<T> {

    /**
     * Specify the item that is to be returned as the subquery result. Replaces the previously specified selection, if
     * any.
     * 
     * @param expression
     *            expression specifying the item that is to be returned as the subquery result
     * @return the modified subquery
     */
    Subquery<T> select(Expression<T> expression);

    /**
     * Modify the subquery to restrict the result according to the specified boolean expression. Replaces the previously
     * added restriction(s), if any. This method only overrides the return type of the corresponding
     * <code>AbstractQuery</code> method.
     * 
     * @param restriction
     *            a simple or compound boolean expression
     * @return the modified subquery
     */
    Subquery<T> where(Expression<Boolean> restriction);

    /**
     * Modify the subquery to restrict the result according to the conjunction of the specified restriction predicates.
     * Replaces the previously added restriction(s), if any. If no restrictions are specified, any previously added
     * restrictions are simply removed. This method only overrides the return type of the corresponding
     * <code>AbstractQuery</code> method.
     * 
     * @param restrictions
     *            zero or more restriction predicates
     * @return the modified subquery
     */
    Subquery<T> where(Predicate... restrictions);

    /**
     * Specify the expressions that are used to form groups over the subquery results. Replaces the previous specified
     * grouping expressions, if any. If no grouping expressions are specified, any previously added grouping expressions
     * are simply removed. This method only overrides the return type of the corresponding <code>AbstractQuery</code>
     * method.
     * 
     * @param grouping
     *            zero or more grouping expressions
     * @return the modified subquery
     */
    Subquery<T> groupBy(Expression<?>... grouping);

    /**
     * Specify the expressions that are used to form groups over the subquery results. Replaces the previous specified
     * grouping expressions, if any. If no grouping expressions are specified, any previously added grouping expressions
     * are simply removed. This method only overrides the return type of the corresponding <code>AbstractQuery</code>
     * method.
     * 
     * @param grouping
     *            list of zero or more grouping expressions
     * @return the modified subquery
     */
    Subquery<T> groupBy(List<Expression<?>> grouping);

    /**
     * Specify a restriction over the groups of the subquery. Replaces the previous having restriction(s), if any. This
     * method only overrides the return type of the corresponding <code>AbstractQuery</code> method.
     * 
     * @param restriction
     *            a simple or compound boolean expression
     * @return the modified subquery
     */
    Subquery<T> having(Expression<Boolean> restriction);

    /**
     * Specify restrictions over the groups of the subquery according the conjunction of the specified restriction
     * predicates. Replaces the previously added having restriction(s), if any. If no restrictions are specified, any
     * previously added restrictions are simply removed. This method only overrides the return type of the corresponding
     * <code>AbstractQuery</code> method.
     * 
     * @param restrictions
     *            zero or more restriction predicates
     * @return the modified subquery
     */
    Subquery<T> having(Predicate... restrictions);

    /**
     * Specify whether duplicate query results will be eliminated. A true value will cause duplicates to be eliminated.
     * A false value will cause duplicates to be retained. If distinct has not been specified, duplicate results must be
     * retained. This method only overrides the return type of the corresponding <code>AbstractQuery</code> method.
     * 
     * @param distinct
     *            boolean value specifying whether duplicate results must be eliminated from the subquery result or
     *            whether they must be retained
     * @return the modified subquery.
     */
    Subquery<T> distinct(boolean distinct);

    /**
     * Return the query of which this is a subquery. This must be a CriteriaQuery or a Subquery.
     * 
     * @return the enclosing query or subquery
     */
    AbstractQuery<?> getParent();

    /**
     * Return the query of which this is a subquery. This may be a CriteriaQuery, CriteriaUpdate, CriteriaDelete, or a
     * Subquery.
     * 
     * @return the enclosing query or subquery
     * @since iBatis Persistence 1.0
     */
    CommonAbstractCriteria getContainingQuery();

    /**
     * Return the selection expression.
     * 
     * @return the item to be returned in the subquery result
     */
    Expression<T> getSelection();

}
