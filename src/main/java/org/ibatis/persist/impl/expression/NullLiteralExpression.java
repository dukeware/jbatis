package org.ibatis.persist.impl.expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Represents a <tt>NULL</tt>literal expression.
 */
public class NullLiteralExpression<T> extends ExpressionImpl<T> {
	public NullLiteralExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> type) {
		super( criteriaBuilder, type );
	}

    public void render(RenderingContext rc) {
        rc.append("null");
    }

    public void renderProjection(RenderingContext rc) {
        render(rc);
        if (getAlias() != null) {
            rc.append(" AS ").append(getAlias());
        }
    }
}
