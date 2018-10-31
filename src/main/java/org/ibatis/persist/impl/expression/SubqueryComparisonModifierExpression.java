package org.ibatis.persist.impl.expression;

import org.ibatis.persist.criteria.Subquery;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Represents a {@link Modifier#ALL}, {@link Modifier#ANY}, {@link Modifier#SOME} modifier appplied to a subquery as
 * part of a comparison.
 */
public class SubqueryComparisonModifierExpression<Y>
		extends ExpressionImpl<Y> {
	public static enum Modifier {
		ALL {
			String rendered() {
				return "all ";
			}
		},
		SOME {
			String rendered() {
				return "some ";
			}
		},
		ANY {
			String rendered() {
				return "any ";
			}
		};
		abstract String rendered();
	}

	private final Subquery<Y> subquery;
	private final Modifier modifier;

	public SubqueryComparisonModifierExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<Y> javaType,
			Subquery<Y> subquery,
			Modifier modifier) {
		super( criteriaBuilder, javaType);
		this.subquery = subquery;
		this.modifier = modifier;
	}

	public Modifier getModifier() {
		return modifier;
	}

	public Subquery<Y> getSubquery() {
		return subquery;
	}

    public void render(RenderingContext rc) {
        rc.append(getModifier().rendered());
        ((Renderable) getSubquery()).render(rc);
    }

    public void renderProjection(RenderingContext rc) {
        throw new IllegalStateException( "Subquery cannot occur in select clause" );
    }
}
