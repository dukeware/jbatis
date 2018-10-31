package org.ibatis.persist.criteria;

/**
 * Represents a bound type, usually an entity that appears in the from clause.
 *
 * @param <Z>
 *            the source type
 * @param <X>
 *            the target type
 *
 * @since iBatis Persistence 1.0
 */
public interface From {

    /**
     * Create an inner join to the specified attribute.
     * 
     * @param attribute
     *            the attribute for the target of the join
     * @return the resulting join
     * @throws IllegalArgumentException
     *             if attribute of the given name does not exist
     */
    Join join(From from);

    /**
     * Create a join to the specified attribute using the given join type.
     * 
     * @param attribute
     *            the attribute for the target of the join
     * @param jt
     *            join type
     * @return the resulting join
     * @throws IllegalArgumentException
     *             if attribute of the given name does not exist
     */
    Join join(From from, JoinType jt);
}
