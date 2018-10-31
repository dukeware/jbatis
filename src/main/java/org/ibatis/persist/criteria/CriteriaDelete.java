package org.ibatis.persist.criteria;

/**
 * The <code>CriteriaDelete</code> interface defines functionality for performing bulk delete operations using the
 * Criteria API
 *
 * <p>
 * Criteria API bulk delete operations map directly to database delete operations. The persistence context is not
 * synchronized with the result of the bulk delete.
 *
 * <p>
 * A <code>CriteriaDelete</code> object must have a single root.
 *
 * @param <T>
 *            the entity type that is the target of the delete
 *
 * @since iBatis Persistence 1.0
 */
public interface CriteriaDelete<T> extends CommonAbstractCriteria, Parameterized<CriteriaDelete<T>> {

    /**
     * Create and add a query root corresponding to the entity that is the target of the delete. A
     * <code>CriteriaDelete</code> object has a single root, the entity that is being deleted.
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
     * Modify the delete query to restrict the target of the deletion according to the specified boolean expression.
     * Replaces the previously added restriction(s), if any.
     * 
     * @param restriction
     *            a simple or compound boolean expression
     * @return the modified delete query
     */
    CriteriaDelete<T> where(Expression<Boolean> restriction);

    /**
     * Modify the delete query to restrict the target of the deletion according to the conjunction of the specified
     * restriction predicates. Replaces the previously added restriction(s), if any. If no restrictions are specified,
     * any previously added restrictions are simply removed.
     * 
     * @param restrictions
     *            zero or more restriction predicates
     * @return the modified delete query
     */
    CriteriaDelete<T> where(Predicate... restrictions);

}
