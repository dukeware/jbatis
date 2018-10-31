package org.ibatis.persist.impl.predicate;

import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.expression.LiteralExpression;

/**
 * Models a SQL <tt>LIKE</tt> expression.
 */
public class LikePredicate extends AbstractSimplePredicate {
	private final Expression<String> matchExpression;
	private final Expression<String> pattern;
	private final Expression<Character> escapeCharacter;

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			Expression<String> pattern) {
		this( criteriaBuilder, matchExpression, pattern, null );
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			String pattern) {
		this( criteriaBuilder, matchExpression, new LiteralExpression<String>( criteriaBuilder, pattern) );
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			Expression<String> pattern,
			Expression<Character> escapeCharacter) {
		super( criteriaBuilder );
		this.matchExpression = matchExpression;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			Expression<String> pattern,
			char escapeCharacter) {
		this(
				criteriaBuilder,
				matchExpression,
				pattern,
				new LiteralExpression<Character>( criteriaBuilder, escapeCharacter )
		);
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			String pattern,
			char escapeCharacter) {
		this(
				criteriaBuilder,
				matchExpression,
				new LiteralExpression<String>( criteriaBuilder, pattern ),
				new LiteralExpression<Character>( criteriaBuilder, escapeCharacter )
		);
	}

	public LikePredicate(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> matchExpression,
			String pattern,
			Expression<Character> escapeCharacter) {
		this(
				criteriaBuilder,
				matchExpression,
				new LiteralExpression<String>( criteriaBuilder, pattern ),
				escapeCharacter
		);
	}

	public Expression<Character> getEscapeCharacter() {
		return escapeCharacter;
	}

	public Expression<String> getMatchExpression() {
		return matchExpression;
	}

	public Expression<String> getPattern() {
		return pattern;
	}

	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        final String operator = isNegated ? " not like " : " like ";
        ((Renderable) getMatchExpression()).render(rc);
        rc.append(operator);
        ((Renderable) getPattern()).render(rc);
        if (escapeCharacter != null) {
            rc.append(" escape ");
            ((Renderable) getEscapeCharacter()).render(rc);
        }
    }
}
