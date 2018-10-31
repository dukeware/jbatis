package org.ibatis.persist.impl.expression;

import org.ibatis.persist.criteria.Expression;

import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;

/**
 * Models standard arithmetc operations with two operands.
 */
public class BinaryArithmeticOperation<N extends Number>
		extends ExpressionImpl<N>
		implements BinaryOperatorExpression<N> {

    public static enum Operation {
        ADD {
            @Override
            void apply(RenderingContext rc, Renderable lhs, Renderable rhs) {
                applyPrimitive(rc, lhs, '+', rhs);
            }
        },
        SUBTRACT {
            @Override
            void apply(RenderingContext rc, Renderable lhs, Renderable rhs) {
                applyPrimitive(rc, lhs, '-', rhs);
            }
        },
        MULTIPLY {
            @Override
            void apply(RenderingContext rc, Renderable lhs, Renderable rhs) {
                applyPrimitive(rc, lhs, '*', rhs);
            }
        },
        DIVIDE {
            @Override
            void apply(RenderingContext rc, Renderable lhs, Renderable rhs) {
                applyPrimitive(rc, lhs, '/', rhs);
            }
        },
        QUOT {
            @Override
            void apply(RenderingContext rc, Renderable lhs, Renderable rhs) {
                applyPrimitive(rc, lhs, '/', rhs);
            }
        },
        MOD {
            @Override
            void apply(RenderingContext rc, Renderable lhs, Renderable rhs) {
                rc.append("mod(");
                lhs.render(rc);
                rc.append(",");
                rhs.render(rc);
                rc.append(")");
            }
        };

        abstract void apply(RenderingContext rc, Renderable lhs, Renderable rhs);

        private static final char LEFT_PAREN = '(';
        private static final char RIGHT_PAREN = ')';

        private static void applyPrimitive(RenderingContext rc, Renderable lhs, char operator, Renderable rhs) {
            rc.append(LEFT_PAREN);
            lhs.render(rc);
            rc.append(operator);
            rhs.render(rc);
            rc.append(RIGHT_PAREN);
        }
    }

	private final Operation operator;
	private final Expression<? extends N> rhs;
	private final Expression<? extends N> lhs;

	/**
	 * Helper for determining the appropriate operation return type based on one of the operands as an expression.
	 *
	 * @param defaultType The default return type to use if we cannot determine the java type of 'expression' operand.
	 * @param expression The operand.
	 *
	 * @return The appropriate return type.
	 */
	public static Class<? extends Number> determineReturnType(
			Class<? extends Number> defaultType,
			Expression<? extends Number> expression) {
		return expression == null || expression.getJavaType() == null 
				? defaultType
				: expression.getJavaType();
	}

	/**
	 * Helper for determining the appropriate operation return type based on one of the operands as a literal.
	 *
	 * @param defaultType The default return type to use if we cannot determine the java type of 'numberLiteral' operand.
	 * @param numberLiteral The operand.
	 *
	 * @return The appropriate return type.
	 */
	public static Class<? extends Number> determineReturnType(
			Class<? extends Number> defaultType,
			Number numberLiteral) {
		return numberLiteral == null ? defaultType : numberLiteral.getClass();
	}

	/**
	 * Creates an arithmethic operation based on 2 expressions.
	 *
	 * @param criteriaBuilder The builder for query components.
	 * @param resultType The operation result type
	 * @param operator The operator (type of operation).
	 * @param lhs The left-hand operand.
	 * @param rhs The right-hand operand
	 */
	public BinaryArithmeticOperation(
			CriteriaBuilderImpl criteriaBuilder,
			Class<N> resultType,
			Operation operator,
			Expression<? extends N> lhs,
			Expression<? extends N> rhs) {
		super( criteriaBuilder, resultType );
		this.operator = operator;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	/**
	 * Creates an arithmethic operation based on an expression and a literal.
	 *
	 * @param criteriaBuilder The builder for query components.
	 * @param javaType The operation result type
	 * @param operator The operator (type of operation).
	 * @param lhs The left-hand operand
	 * @param rhs The right-hand operand (the literal)
	 */
	public BinaryArithmeticOperation(
			CriteriaBuilderImpl criteriaBuilder,
			Class<N> javaType,
			Operation operator,
			Expression<? extends N> lhs,
			N rhs) {
		super( criteriaBuilder, javaType );
		this.operator = operator;
		this.lhs = lhs;
		this.rhs = new LiteralExpression<N>( criteriaBuilder, rhs );
	}

	/**
	 * Creates an arithmetic operation based on an expression and a literal.
	 *
	 * @param criteriaBuilder The builder for query components.
	 * @param javaType The operation result type
	 * @param operator The operator (type of operation).
	 * @param lhs The left-hand operand (the literal)
	 * @param rhs The right-hand operand
	 */
	public BinaryArithmeticOperation(
			CriteriaBuilderImpl criteriaBuilder,
			Class<N> javaType,
			Operation operator,
			N lhs,
			Expression<? extends N> rhs) {
		super( criteriaBuilder, javaType );
		this.operator = operator;
		this.lhs = new LiteralExpression<N>( criteriaBuilder, lhs );
		this.rhs = rhs;
	}
	public Operation getOperator() {
		return operator;
	}

	@Override
	public Expression<? extends N> getRightHandOperand() {
		return rhs;
	}

	@Override
	public Expression<? extends N> getLeftHandOperand() {
		return lhs;
	}

	@Override
    public void render(RenderingContext rc) {
        getOperator().apply(rc, (Renderable) getLeftHandOperand(), (Renderable) getRightHandOperand());
    }

	@Override
    public void renderProjection(RenderingContext rc) {
        render(rc);
        if (getAlias() != null) {
            rc.append(" AS ").append(getAlias());
        }
    }
}
