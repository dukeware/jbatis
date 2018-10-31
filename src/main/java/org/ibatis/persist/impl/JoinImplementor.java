package org.ibatis.persist.impl;

import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Join;
import org.ibatis.persist.criteria.Predicate;

public interface JoinImplementor extends Join/*, Fetch<Z,X>*/, FromImplementor {
    
    FromImplementor getLeft();
    
    FromImplementor getRight();

	/**
	 * Coordinate return type between {@link Join#on(Expression)} and {@link Fetch#on(Expression)}
	 */
	@Override
	public JoinImplementor on(Expression<Boolean> restriction);

	/**
	 * Coordinate return type between {@link Join#on(Predicate...)} and {@link Fetch#on(Predicate...)}
	 */
	@Override
	public JoinImplementor on(Predicate... restrictions);

}
