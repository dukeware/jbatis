package org.ibatis.persist.impl.predicate;

import org.ibatis.persist.criteria.Subquery;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Models an <tt>EXISTS(<subquery>)</tt> predicate
 */
public class ExistsPredicate
		extends AbstractSimplePredicate {
	private final Subquery<?> subquery;

	public ExistsPredicate(CriteriaBuilderImpl criteriaBuilder, Subquery<?> subquery) {
		super( criteriaBuilder );
		this.subquery = subquery;
	}

	public Subquery<?> getSubquery() {
		return subquery;
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        rc.append(isNegated ? "not " : "").append("exists ");
        ((Renderable) getSubquery()).render(rc);
    }
}
