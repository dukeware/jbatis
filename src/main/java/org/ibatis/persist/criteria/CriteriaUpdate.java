package org.ibatis.persist.criteria;

/**
 * The <code>CriteriaUpdate</code> interface defines functionality for performing bulk update operations using the
 * Criteria API.
 *
 * <p>
 * Criteria API bulk update operations map directly to database update operations, bypassing any optimistic locking
 * checks. Portable applications using bulk update operations must manually update the value of the version column, if
 * desired, and/or manually validate the value of the version column. The persistence context is not synchronized with
 * the result of the bulk update.
 *
 * <p>
 * A <code>CriteriaUpdate</code> object must have a single root.
 *
 * @param <T>
 *            the entity type that is the target of the update
 *
 * @since iBatis Persistence 1.0
 */
public interface CriteriaUpdate<T> extends CommonAbstractCriteria, Parameterized<CriteriaUpdate<T>> {

    /**
     * Create and add a query root corresponding to the entity that is the target of the update. A
     * <code>CriteriaUpdate</code> object has a single root, the entity that is being updated.
     * 
     * @param entityClass
     *            the entity class
     * @return query root corresponding to the given entity
     */
    Root<T> from(Class<T> entityClass);

    /**
     * Return the query root.
     * 
     * @return the query root
     */
    Root<T> getRoot();

    /**
     * Update the value of the specified attribute.
     * 
     * @param attribute
     *            attribute to be updated
     * @param value
     *            new value
     * @return the modified update query
     */
    <Y, X extends Y> CriteriaUpdate<T> set(Path<Y> attribute, X value);

    /**
     * Update the value of the specified attribute.
     * 
     * @param attribute
     *            attribute to be updated
     * @param value
     *            new value
     * @return the modified update query
     */
    <Y> CriteriaUpdate<T> set(Path<Y> attribute, Expression<? extends Y> value);

    /**
     * Update the value of the specified attribute.
     * 
     * @param attribute
     *            the attribute to be updated
     * @param value
     *            new value
     * @return the modified update query
     */
    <Y> CriteriaUpdate<T> set(Y attribute, Expression<? extends Y> value);

    /**
     * Update the value of the specified attribute.
     * 
     * @param attribute
     *            the attribute to be updated
     * @param value
     *            new value
     * @return the modified update query
     */
    <Y> CriteriaUpdate<T> set(Y attribute, Y value);

    /**
     * Modify the update query to restrict the target of the update according to the specified boolean expression.
     * Replaces the previously added restriction(s), if any.
     * 
     * @param restriction
     *            a simple or compound boolean expression
     * @return the modified update query
     */
    CriteriaUpdate<T> where(Expression<Boolean> restriction);

    /**
     * Modify the update query to restrict the target of the update according to the conjunction of the specified
     * restriction predicates. Replaces the previously added restriction(s), if any. If no restrictions are specified,
     * any previously added restrictions are simply removed.
     * 
     * @param restrictions
     *            zero or more restriction predicates
     * @return the modified update query
     */
    CriteriaUpdate<T> where(Predicate... restrictions);
}
