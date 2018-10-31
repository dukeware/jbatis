package org.ibatis.persist.meta;

import java.util.List;
import java.util.Map;

/**
 * Instances of the type <code>EntityType</code> represent entity types.
 *
 * @param <X>
 *            The represented entity type.
 *
 * @since iBatis Persistence 1.0
 */
public interface EntityType<E> {

    /**
     * Return the entity name.
     * 
     * @return entity name
     */
    String getName();

    boolean isFailed();

    boolean isCacheable();
    
    String getEntityCacheModelId();

    Map<String, Attribute<E, ?>> getIdAttributes();

    Map<String, Attribute<E, ?>> getAttributes();

    String getErrorMessage();

    String getInsertStatementId();

    Object getInsertParameter(E e);

    String getUpdateStatementId();

    Object getUpdateParameter(E e);

    String getDeleteStatementId();

    Object getDeleteParameter(Object key);

    String getFindStatementId();

    Object getFindParameter(Object key);

    Class<E> getJavaType();

    Attribute<E, ?> locateAttribute(String name);

    List<String> getAttributeNames();

    String getTableName();

}
