package org.ibatis.persist.impl.predicate;

import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.expression.BinaryOperatorExpression;
import org.ibatis.persist.impl.expression.LiteralExpression;

import com.ibatis.sqlmap.engine.type.TypeHandlerFactory;

/**
 * Models a basic relational comparison predicate.
 */
@SuppressWarnings("unchecked")
public class ComparisonPredicate
		extends AbstractSimplePredicate
		implements BinaryOperatorExpression<Boolean> {
	private final ComparisonOperator comparisonOperator;
	private final Expression<?> leftHandSide;
	private final Expression<?> rightHandSide;

	public ComparisonPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			ComparisonOperator comparisonOperator,
			Expression<?> leftHandSide,
			Expression<?> rightHandSide) {
		super( criteriaBuilder );
		this.comparisonOperator = comparisonOperator;
		this.leftHandSide = leftHandSide;
		this.rightHandSide = rightHandSide;
	}

	public ComparisonPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			ComparisonOperator comparisonOperator,
			Expression<?> leftHandSide,
			Object rightHandSide) {
		super( criteriaBuilder );
		this.comparisonOperator = comparisonOperator;
		this.leftHandSide = leftHandSide;
        if (TypeHandlerFactory.isNumeric(leftHandSide.getJavaType())) {
            this.rightHandSide = new LiteralExpression(criteriaBuilder, leftHandSide.getJavaType(), rightHandSide);
        }
		else {
			this.rightHandSide = new LiteralExpression( criteriaBuilder, rightHandSide );
		}
	}

	public <N extends Number> ComparisonPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			ComparisonOperator comparisonOperator,
			Expression<N> leftHandSide,
			Number rightHandSide) {
		super( criteriaBuilder );
		this.comparisonOperator = comparisonOperator;
		this.leftHandSide = leftHandSide;
		
		this.rightHandSide = new LiteralExpression( criteriaBuilder, rightHandSide );
	}

	public ComparisonOperator getComparisonOperator() {
		return getComparisonOperator( isNegated() );
	}

	public ComparisonOperator getComparisonOperator(boolean isNegated) {
		return isNegated
				? comparisonOperator.negated()
				: comparisonOperator;
	}

	@Override
	public Expression getLeftHandOperand() {
		return leftHandSide;
	}

	@Override
	public Expression getRightHandOperand() {
		return rightHandSide;
	}

	/**
	 * Defines the comparison operators.  We could also get away with
	 * only 3 and use negation...
	 */
	public static enum ComparisonOperator {
		EQUAL {
			public ComparisonOperator negated() {
				return NOT_EQUAL;
			}
			public String rendered() {
				return " = ";
			}
		},
		NOT_EQUAL {
			public ComparisonOperator negated() {
				return EQUAL;
			}
			public String rendered() {
				return " <> ";
			}
		},
		LESS_THAN {
			public ComparisonOperator negated() {
				return GREATER_THAN_OR_EQUAL;
			}
			public String rendered() {
				return " < ";
			}
		},
		LESS_THAN_OR_EQUAL {
			public ComparisonOperator negated() {
				return GREATER_THAN;
			}
			public String rendered() {
				return " <= ";
			}
		},
		GREATER_THAN {
			public ComparisonOperator negated() {
				return LESS_THAN_OR_EQUAL;
			}
			public String rendered() {
				return " > ";
			}
		},
		GREATER_THAN_OR_EQUAL {
			public ComparisonOperator negated() {
				return LESS_THAN;
			}
			public String rendered() {
				return " >= ";
			}
		};

		public abstract ComparisonOperator negated();

		public abstract String rendered();
	}


	@Override
    public void render(boolean isNegated, RenderingContext rc) {
        ((Renderable) getLeftHandOperand()).render(rc);
        rc.append(getComparisonOperator(isNegated).rendered());
        ((Renderable) getRightHandOperand()).render(rc);
    }
}
