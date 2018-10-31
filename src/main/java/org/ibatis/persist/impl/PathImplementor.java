package org.ibatis.persist.impl;

import org.ibatis.persist.criteria.Path;
import org.ibatis.persist.meta.Attribute;

/**
 * Implementation contract for the JPA {@link Path} interface.
 */
public interface PathImplementor<X> extends ExpressionImplementor<X>, Path<X> {
	/**
	 * Retrieve reference to the attribute this path represents.
	 *
	 * @return The metamodel attribute.
	 */
	public Attribute<?, ?> getAttribute();

}
