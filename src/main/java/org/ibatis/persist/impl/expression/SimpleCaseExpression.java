package org.ibatis.persist.impl.expression;

import java.util.ArrayList;
import java.util.List;
import org.ibatis.persist.criteria.CriteriaBuilder.SimpleCase;
import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Models what ANSI SQL terms a simple case statement.  This is a <tt>CASE</tt> expression in the form<pre>
 * CASE [expression]
 *     WHEN [firstCondition] THEN [firstResult]
 *     WHEN [secondCondition] THEN [secondResult]
 *     ELSE [defaultResult]
 * END
 * </pre>
 */
public class SimpleCaseExpression<C,R>
		extends ExpressionImpl<R>
		implements SimpleCase<C,R> {
	private Class<R> javaType;
	private final Expression<? extends C> expression;
	private List<WhenClause> whenClauses = new ArrayList<WhenClause>();
	private Expression<? extends R> otherwiseResult;

	public class WhenClause {
		private final LiteralExpression<C> condition;
		private final Expression<? extends R> result;

		public WhenClause(LiteralExpression<C> condition, Expression<? extends R> result) {
			this.condition = condition;
			this.result = result;
		}

		public LiteralExpression<C> getCondition() {
			return condition;
		}

		public Expression<? extends R> getResult() {
			return result;
		}

	}

	public SimpleCaseExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<R> javaType,
			Expression<? extends C> expression) {
		super( criteriaBuilder, javaType);
		this.javaType = javaType;
		this.expression = expression;
	}

	@SuppressWarnings({ "unchecked" })
	public Expression<C> getExpression() {
		return (Expression<C>) expression;
	}

	public SimpleCase<C, R> when(C condition, R result) {
		return when( condition, buildLiteral(result) );
	}

	@SuppressWarnings({ "unchecked" })
	private LiteralExpression<R> buildLiteral(R result) {
		final Class<R> type = result != null
				? (Class<R>) result.getClass()
				: getJavaType();
		return new LiteralExpression<R>( criteriaBuilder(), type, result );
	}

	public SimpleCase<C, R> when(C condition, Expression<? extends R> result) {
		WhenClause whenClause = new WhenClause(
				new LiteralExpression<C>( criteriaBuilder(), condition ),
				result
		);
		whenClauses.add( whenClause );
		adjustJavaType( result );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	private void adjustJavaType(Expression<? extends R> exp) {
		if ( javaType == null ) {
			javaType = (Class<R>) exp.getJavaType();
		}
	}

	public Expression<R> otherwise(R result) {
		return otherwise( buildLiteral(result) );
	}

	public Expression<R> otherwise(Expression<? extends R> result) {
		this.otherwiseResult = result;
		adjustJavaType( result );
		return this;
	}

	public Expression<? extends R> getOtherwiseResult() {
		return otherwiseResult;
	}

	public List<WhenClause> getWhenClauses() {
		return whenClauses;
	}

    public void render(RenderingContext rc) {
        rc.append("case ");
        ((Renderable) getExpression()).render(rc);
        for (WhenClause whenClause : getWhenClauses()) {
            rc.append(" when ");
            whenClause.getCondition().render(rc);
            rc.append(" then ");
            ((Renderable) whenClause.getResult()).render(rc);
        }
        rc.append(" else ");
        ((Renderable) getOtherwiseResult()).render(rc);
        rc.append(" end");
    }

    public void renderProjection(RenderingContext rc) {
        render(rc);
        if (getAlias() != null) {
            rc.append(" AS ").append(getAlias());
        }
    }
}
