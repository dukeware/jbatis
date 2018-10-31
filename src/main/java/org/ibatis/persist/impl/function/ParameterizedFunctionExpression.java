package org.ibatis.persist.impl.function;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Support for functions with parameters.
 */
@SuppressWarnings("unchecked")
public class ParameterizedFunctionExpression<X>
		extends BasicFunctionExpression<X>
		implements FunctionExpression<X> {

	public static List<String> STANDARD_JPA_FUNCTION_NAMES = Arrays.asList(
			// 4.6.17.2.1
			"CONCAT",
			"SUBSTRING",
			"TRIM",
			"UPPER",
			"LOWER",
			"LOCATE",
			"LENGTH",
			//4.6.17.2.2
			"ABS",
			"SQRT",
			"MOD",
			"SIZE",
			"INDEX",
			// 4.6.17.2.3
			"CURRENT_DATE",
			"CURRENT_TIME",
			"CURRENT_TIMESTAMP"
	);

	private final List<Expression<?>> argumentExpressions;
	private final boolean isStandardJpaFunction;

	public ParameterizedFunctionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			String functionName,
			List<Expression<?>> argumentExpressions) {
		super( criteriaBuilder, javaType, functionName );
		this.argumentExpressions = argumentExpressions;
		this.isStandardJpaFunction = STANDARD_JPA_FUNCTION_NAMES.contains( functionName.toUpperCase(Locale.ROOT) );
	}

	public ParameterizedFunctionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			String functionName,
			Expression<?>... argumentExpressions) {
		super( criteriaBuilder, javaType, functionName );
		this.argumentExpressions = Arrays.asList( argumentExpressions );
		this.isStandardJpaFunction = STANDARD_JPA_FUNCTION_NAMES.contains( functionName.toUpperCase(Locale.ROOT) );
	}

	protected boolean isStandardJpaFunction() {
		return isStandardJpaFunction;
	}

	protected  static int properSize(int number) {
		return number + (int)( number*.75 ) + 1;
	}

	public List<Expression<?>> getArgumentExpressions() {
		return argumentExpressions;
	}

	@Override
    public void render(RenderingContext rc) {
        if (isStandardJpaFunction()) {
            rc.append(getFunctionName()).append("(");
        } else {
            rc.append("function('").append(getFunctionName()).append("', ");
        }
        renderArguments(rc);
        rc.append(')');
    }

    protected void renderArguments(RenderingContext rc) {
        String sep = "";
        for (Expression argument : argumentExpressions) {
            rc.append(sep);
            ((Renderable) argument).render(rc);
            sep = ", ";
        }
    }

}
