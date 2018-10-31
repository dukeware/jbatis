package org.ibatis.persist.impl;

import org.ibatis.persist.impl.RenderingContext;

/**
 * Implementation contract for things which can be the source (parent, left-hand-side, etc) of a path
 */
public interface PathSource {
    
	public void prepareAlias(RenderingContext rc);

	public String getPathAlias();
}
