package org.ibatis.persist.impl.predicate;

import org.ibatis.persist.criteria.Predicate;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

public interface PredicateImplementor extends Predicate {
	/**
	 * Access to the CriteriaBuilder
	 *
	 * @return The CriteriaBuilder
	 */
	public CriteriaBuilderImpl criteriaBuilder();

	/**
	 * Is this a conjunction or disjunction?
	 *
	 * @return {@code true} if this predicate is a junction (AND/OR); {@code false} otherwise
	 */
	public boolean isJunction();

	/**
	 * Form of {@link Renderable#render} used when the predicate is wrapped in a negated wrapper.  Allows passing
	 * down the negation flag.
	 * <p/>
	 * Note that this form is no-op in compound (junction) predicates.  The reason being that compound predicates
	 * are more complex and the negation is applied during its creation.
	 *
	 * @param isNegated Should the predicate be negated.
	 * @param rc The context for rendering
	 */
	public void render(boolean isNegated, RenderingContext rc);
}
