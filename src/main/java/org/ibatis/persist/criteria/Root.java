package org.ibatis.persist.criteria;

import org.ibatis.persist.meta.EntityType;

/**
 * A root type in the from clause. Query roots always reference entities.
 *
 * @param <X>
 *            the entity type referenced by the root
 *
 * @since iBatis Persistence 1.0
 */
public interface Root<X> extends From {
    /**
     * Make a template root object.
     */
    X $();

    /**
     * Return the metamodel entity corresponding to the root.
     * @return metamodel entity corresponding to the root
     */
    EntityType<X> getModel();

    /**
     * Create a path corresponding to the referenced attribute.
     * 
     * @param attribute
     *            the attribute
     */
    <Y> Path<Y> get(Y attribute);


    Root<X> alias(String name);

    /**
     * Return the alias assigned to the tuple element or null, if no alias has been assigned.
     * 
     * @return alias
     */
    String getAlias();
}
