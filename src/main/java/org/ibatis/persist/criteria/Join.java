package org.ibatis.persist.criteria;

/**
 * A join to an entity or basic type.
 *
 * @param <Z>
 *            the source type of the join
 * @param <X>
 *            the target type of the join
 *
 * @since iBatis Persistence 1.0
 */
public interface Join extends From {

    /**
     * Modify the join to restrict the result according to the specified ON condition and return the join object.
     * Replaces the previous ON condition, if any.
     * 
     * @param restriction
     *            a simple or compound boolean expression
     * @return the modified join object
     */
    Join on(Expression<Boolean> restriction);

    /**
     * Modify the join to restrict the result according to the specified ON condition and return the join object.
     * Replaces the previous ON condition, if any.
     * 
     * @param restrictions
     *            zero or more restriction predicates
     * @return the modified join object
     */
    Join on(Predicate... restrictions);

    /**
     * Return the predicate that corresponds to the ON restriction(s) on the join, or null if no ON condition has been
     * specified.
     * 
     * @return the ON restriction predicate
     */
    Predicate getOn();

    /**
     * Return the join type.
     * 
     * @return join type
     */
    JoinType getJoinType();
}
