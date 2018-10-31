package org.ibatis.persist.impl.function;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.expression.ExpressionImpl;

/**
 * Models the basic concept of a SQL function.
 */
public class BasicFunctionExpression<X>
		extends ExpressionImpl<X>
		implements FunctionExpression<X> {

	private final String functionName;

	public BasicFunctionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			String functionName) {
		super( criteriaBuilder, javaType );
		this.functionName = functionName;
	}

	protected  static int properSize(int number) {
		return number + (int)( number*.75 ) + 1;
	}

	public String getFunctionName() {
		return functionName;
	}

	public boolean isAggregation() {
		return false;
	}

	public void render(RenderingContext rc) {
	    rc.append(getFunctionName()).append("()");
	}

	public void renderProjection(RenderingContext rc) {
	    render(rc);
        if (getAlias() != null) {
            rc.append(" AS ").append(getAlias());
        }
	}
}
