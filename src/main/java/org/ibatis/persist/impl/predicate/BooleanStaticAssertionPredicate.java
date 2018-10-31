package org.ibatis.persist.impl.predicate;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Predicate used to assert a static boolean condition.
 */
public class BooleanStaticAssertionPredicate
		extends AbstractSimplePredicate {
	private final Boolean assertedValue;

	public BooleanStaticAssertionPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Boolean assertedValue) {
		super( criteriaBuilder );
		this.assertedValue = assertedValue;
	}

	public Boolean getAssertedValue() {
		return assertedValue;
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        boolean isTrue = getAssertedValue();
        if (isNegated) {
            isTrue = !isTrue;
        }
        rc.append(isTrue ? "1=1" : "0=1");
    }

}
