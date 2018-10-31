package org.ibatis.persist.impl.predicate;

import java.util.List;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.criteria.Selection;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.expression.ExpressionImpl;

/**
 * Basic template support for {@link Predicate} implementors providing
 * expression handling, negation and conjunction/disjunction handling.
 */
public abstract class AbstractPredicateImpl
		extends ExpressionImpl<Boolean>
		implements PredicateImplementor {

	protected AbstractPredicateImpl(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder, Boolean.class );
	}

	public boolean isNegated() {
		return false;
	}

	public Predicate not() {
		return new NegatedPredicateWrapper( this );
	}


	// Selection ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public final boolean isCompoundSelection() {
		// Should always be false for predicates
		return super.isCompoundSelection();
	}

	@Override
	public final List<Selection<?>> getCompoundSelectionItems() {
		// Should never have sub selection items for predicates
		return super.getCompoundSelectionItems();
	}

}
