package org.ibatis.persist.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.ibatis.persist.criteria.CompoundSelection;
import org.ibatis.persist.criteria.CriteriaBuilder;
import org.ibatis.persist.criteria.CriteriaDelete;
import org.ibatis.persist.criteria.CriteriaQuery;
import org.ibatis.persist.criteria.CriteriaUpdate;
import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Order;
import org.ibatis.persist.criteria.ParameterExpression;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.criteria.Selection;
import org.ibatis.persist.criteria.Subquery;
import org.ibatis.persist.impl.expression.BinaryArithmeticOperation;
import org.ibatis.persist.impl.expression.CoalesceExpression;
import org.ibatis.persist.impl.expression.CompoundSelectionImpl;
import org.ibatis.persist.impl.expression.ConcatExpression;
import org.ibatis.persist.impl.expression.LiteralExpression;
import org.ibatis.persist.impl.expression.NullLiteralExpression;
import org.ibatis.persist.impl.expression.NullifExpression;
import org.ibatis.persist.impl.expression.ParameterExpressionImpl;
import org.ibatis.persist.impl.expression.SearchedCaseExpression;
import org.ibatis.persist.impl.expression.SimpleCaseExpression;
import org.ibatis.persist.impl.expression.SubqueryComparisonModifierExpression;
import org.ibatis.persist.impl.expression.UnaryArithmeticOperation;
import org.ibatis.persist.impl.function.AbsFunction;
import org.ibatis.persist.impl.function.AggregationFunction;
import org.ibatis.persist.impl.function.BasicFunctionExpression;
import org.ibatis.persist.impl.function.CurrentDateFunction;
import org.ibatis.persist.impl.function.CurrentTimeFunction;
import org.ibatis.persist.impl.function.CurrentTimestampFunction;
import org.ibatis.persist.impl.function.ParameterizedFunctionExpression;
import org.ibatis.persist.impl.function.SqrtFunction;
import org.ibatis.persist.impl.predicate.BetweenPredicate;
import org.ibatis.persist.impl.predicate.BooleanAssertionPredicate;
import org.ibatis.persist.impl.predicate.BooleanExpressionPredicate;
import org.ibatis.persist.impl.predicate.BooleanStaticAssertionPredicate;
import org.ibatis.persist.impl.predicate.ComparisonPredicate;
import org.ibatis.persist.impl.predicate.ComparisonPredicate.ComparisonOperator;
import org.ibatis.persist.impl.util.TypeHelper;
import org.ibatis.persist.impl.predicate.CompoundPredicate;
import org.ibatis.persist.impl.predicate.ExistsPredicate;
import org.ibatis.persist.impl.predicate.InPredicate;
import org.ibatis.persist.impl.predicate.LikePredicate;
import org.ibatis.persist.impl.predicate.NullnessPredicate;

/**
 * Hibernate implementation of the JPA {@link CriteriaBuilder} contract.
 */
public class CriteriaBuilderImpl implements CriteriaBuilder {
	private final EntityManager entityManager;

	public CriteriaBuilderImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * Provides protected access to the underlying {@link EntityManagerFactoryImpl}.
	 *
	 * @return The underlying {@link EntityManagerFactoryImpl}
	 */
	public  EntityManager getEntityManager() {
		return entityManager;
	}

	// Query builders ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <T> CriteriaQuery<T> createQuery(Class<T> resultClass) {
		return new CriteriaQueryImpl<T>( this, resultClass );
	}

	@Override
	public <T> CriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity) {
		return new CriteriaUpdateImpl<T>( this, targetEntity );
	}

	@Override
	public <T> CriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity) {
		return new CriteriaDeleteImpl<T>( this, targetEntity );
	}


	// selections ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	void checkMultiselect(List<Selection<?>> selections) {
		final HashSet<String> aliases = new HashSet<String>( selections.size() );

		for ( Selection<?> selection : selections ) {
			if ( selection.isCompoundSelection() ) {
				if ( selection.getJavaType().isArray() ) {
					throw new IllegalArgumentException(
							"Selection items in a multi-select cannot contain compound array-valued elements"
					);
				}
			}
            if (selection.getAlias() != null && !selection.getAlias().isEmpty()) {
				boolean added = aliases.add( selection.getAlias() );
				if ( ! added ) {
					throw new IllegalArgumentException( "Multi-select expressions defined duplicate alias : " + selection.getAlias() );
				}
			}
		}
	}

	<Y> CompoundSelection<Y> construct(Class<Y> result, Selection<?>... selections) {
		return construct( result, Arrays.asList( selections ) );
	}

	/**
	 * Version of {@link #construct(Class,Selection[])} taking the
	 * to-be-constructed type as well as a list of selections.
	 *
	 * @param result The result class to be constructed.
	 * @param selections The selections to use in the constructor call.
	 *
	 * @return The <b>view</b> compound selection.
	 */
	<Y> CompoundSelection<Y> construct(Class<Y> result, List<Selection<?>> selections) {
		checkMultiselect( selections );
		return new CompoundSelectionImpl<Y>( this, result, selections );
	}


	// ordering ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Order asc(Expression<?> x) {
		return new OrderImpl( x, true );
	}

	@Override
	public Order desc(Expression<?> x) {
		return new OrderImpl( x, false );
	}


	// predicates ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Predicate wrap(Expression<Boolean> expression) {
		if ( Predicate.class.isInstance( expression ) ) {
			return ( (Predicate) expression );
		}
		else if ( PathImplementor.class.isInstance( expression ) ) {
			return new BooleanAssertionPredicate( this, expression, Boolean.TRUE );
		}
		else {
			return new BooleanExpressionPredicate( this, expression );
		}
	}

	@Override
	public Predicate not(Expression<Boolean> expression) {
		return wrap( expression ).not();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Predicate and(Expression<Boolean> x, Expression<Boolean> y) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.AND, x, y );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Predicate or(Expression<Boolean> x, Expression<Boolean> y) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.OR, x, y );
	}

	@Override
	public Predicate and(Predicate... restrictions) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.AND, restrictions );
	}

	@Override
	public Predicate or(Predicate... restrictions) {
		return new CompoundPredicate( this, Predicate.BooleanOperator.OR, restrictions );
	}

	@Override
	public Predicate conjunction() {
		return new CompoundPredicate( this, Predicate.BooleanOperator.AND );
	}

	@Override
	public Predicate disjunction() {
		return new CompoundPredicate( this, Predicate.BooleanOperator.OR );
	}

	@Override
	public Predicate isTrue(Expression<Boolean> expression) {
		if ( CompoundPredicate.class.isInstance( expression ) ) {
			final CompoundPredicate predicate = (CompoundPredicate) expression;
			if ( predicate.getExpressions().size() == 0 ) {
				return new BooleanStaticAssertionPredicate(
						this,
						predicate.getOperator() == Predicate.BooleanOperator.AND
				);
			}
			return predicate;
		}
		else if ( Predicate.class.isInstance( expression ) ) {
			return (Predicate) expression;
		}
		return new BooleanAssertionPredicate( this, expression, Boolean.TRUE );
	}

	@Override
	public Predicate isFalse(Expression<Boolean> expression) {
		if ( CompoundPredicate.class.isInstance( expression ) ) {
			final CompoundPredicate predicate = (CompoundPredicate) expression;
			if ( predicate.getExpressions().size() == 0 ) {
				return new BooleanStaticAssertionPredicate(
						this,
						predicate.getOperator() == Predicate.BooleanOperator.OR
				);
			}
			predicate.not();
			return predicate;
		}
		else if ( Predicate.class.isInstance( expression ) ) {
			final Predicate predicate = (Predicate) expression;
			predicate.not();
			return predicate;
		}
		return new BooleanAssertionPredicate( this, expression, Boolean.FALSE );
	}

	@Override
	public Predicate isNull(Expression<?> x) {
		return new NullnessPredicate( this, x );
	}

	@Override
	public Predicate isNotNull(Expression<?> x) {
		return isNull( x ).not();
	}

	@Override
	public Predicate equal(Expression<?> x, Expression<?> y) {
		return new ComparisonPredicate( this, ComparisonOperator.EQUAL, x, y );
	}

	@Override
	public Predicate notEqual(Expression<?> x, Expression<?> y) {
		return new ComparisonPredicate( this, ComparisonOperator.NOT_EQUAL, x, y );
	}

	@Override
	public Predicate equal(Expression<?> x, Object y) {
		return new ComparisonPredicate( this, ComparisonOperator.EQUAL, x, y );
	}

	@Override
	public Predicate notEqual(Expression<?> x, Object y) {
		return new ComparisonPredicate( this, ComparisonOperator.NOT_EQUAL, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate lessThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate greaterThan(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate lessThan(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	public<Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Y y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	public Predicate gt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	public Predicate lt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	public Predicate ge(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	public Predicate le(Expression<? extends Number> x, Expression<? extends Number> y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	public Predicate gt(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN, x, y );
	}

	@Override
	public Predicate lt(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN, x, y );
	}

	@Override
	public Predicate ge(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.GREATER_THAN_OR_EQUAL, x, y );
	}

	@Override
	public Predicate le(Expression<? extends Number> x, Number y) {
		return new ComparisonPredicate( this, ComparisonOperator.LESS_THAN_OR_EQUAL, x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate between(
			Expression<? extends Y> expression,
			Y lowerBound,
			Y upperBound) {
		return new BetweenPredicate<Y>( this, expression, lowerBound, upperBound );
	}

	@Override
	public <Y extends Comparable<? super Y>> Predicate between(
			Expression<? extends Y> expression,
			Expression<? extends Y> lowerBound,
			Expression<? extends Y> upperBound) {
		return new BetweenPredicate<Y>( this, expression, lowerBound, upperBound );
	}

	@Override
	public <T> In<T> in(Expression<? extends T> expression) {
		return new InPredicate<T>( this, expression );
	}

    @SuppressWarnings("unchecked")
    public <T> In<T> in(Expression<? extends T> expression, Expression<? extends T>... values) {
		return new InPredicate<T>( this, expression, values );
	}

    @SuppressWarnings("unchecked")
	public <T> In<T> in(Expression<? extends T> expression, T... values) {
		return new InPredicate<T>( this, expression, values );
	}

	public <T> In<T> in(Expression<? extends T> expression, Collection<T> values) {
		return new InPredicate<T>( this, expression, values );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, Expression<String> pattern) {
		return new LikePredicate( this, matchExpression, pattern );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, Expression<String> pattern, Expression<Character> escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, Expression<String> pattern, char escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, String pattern) {
		return new LikePredicate( this, matchExpression, pattern );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, String pattern, Expression<Character> escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public Predicate like(Expression<String> matchExpression, String pattern, char escapeCharacter) {
		return new LikePredicate( this, matchExpression, pattern, escapeCharacter );
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, Expression<String> pattern) {
		return like( matchExpression, pattern ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, Expression<String> pattern, Expression<Character> escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, Expression<String> pattern, char escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, String pattern) {
		return like( matchExpression, pattern ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, String pattern, Expression<Character> escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}

	@Override
	public Predicate notLike(Expression<String> matchExpression, String pattern, char escapeCharacter) {
		return like( matchExpression, pattern, escapeCharacter ).not();
	}


	// parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <T> ParameterExpression<T> parameter(Class<T> paramClass) {
		return new ParameterExpressionImpl<T>( this, paramClass );
	}

	@Override
	public <T> ParameterExpression<T> parameter(Class<T> paramClass, String name) {
		return new ParameterExpressionImpl<T>( this, paramClass, name );
	}

	@Override
	public <T> Expression<T> literal(T value) {
		if ( value == null ) {
			throw new IllegalArgumentException( "literal value cannot be null" );
		}
		return new LiteralExpression<T>( this, value );
	}

	@Override
	public <T> Expression<T> nullLiteral(Class<T> resultClass) {
		return new NullLiteralExpression<T>( this, resultClass );
	}


	// aggregate functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <N extends Number> Expression<Double> avg(Expression<N> x) {
		return new AggregationFunction.AVG( this, x );
	}

	@Override
	public <N extends Number> Expression<N> sum(Expression<N> x) {
		return new AggregationFunction.SUM<N>( this, x );
	}

	@Override
	public Expression<Long> sumAsLong(Expression<Integer> x) {
		return new AggregationFunction.SUM<Long>( this, x, Long.class );
	}

	@Override
	public Expression<Double> sumAsDouble(Expression<Float> x) {
		return new AggregationFunction.SUM<Double>( this, x, Double.class );
	}

	@Override
	public <N extends Number> Expression<N> max(Expression<N> x) {
		return new AggregationFunction.MAX<N>( this, x );
	}

	@Override
	public <N extends Number> Expression<N> min(Expression<N> x) {
		return new AggregationFunction.MIN<N>( this, x );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X extends Comparable<? super X>> Expression<X> greatest(Expression<X> x) {
		return new AggregationFunction.GREATEST( this, x );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X extends Comparable<? super X>> Expression<X> least(Expression<X> x) {
		return new AggregationFunction.LEAST( this, x );
	}

	@Override
	public Expression<Long> count(Expression<?> x) {
		return new AggregationFunction.COUNT( this, x, false );
	}

	@Override
	public Expression<Long> countDistinct(Expression<?> x) {
		return new AggregationFunction.COUNT( this, x, true );
	}


	// other functions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <T> Expression<T> function(String name, Class<T> returnType, Expression<?>... arguments) {
		return new ParameterizedFunctionExpression<T>( this, returnType, name, arguments );
	}

	/**
	 * Create a reference to a function taking no params.
	 *
	 * @param name The function name.
	 * @param returnType The return type.
	 *
	 * @return The function expression
	 */
	public <T> Expression<T> function(String name, Class<T> returnType) {
		return new BasicFunctionExpression<T>( this, returnType, name );
	}

	@Override
	public <N extends Number> Expression<N> abs(Expression<N> expression) {
		return new AbsFunction<N>( this, expression );
	}

	@Override
	public Expression<Double> sqrt(Expression<? extends Number> expression) {
		return new SqrtFunction( this, expression );
	}

	@Override
	public Expression<java.sql.Date> currentDate() {
		return new CurrentDateFunction( this );
	}

	@Override
	public Expression<java.sql.Timestamp> currentTimestamp() {
		return new CurrentTimestampFunction( this );
	}

	@Override
	public Expression<java.sql.Time> currentTime() {
		return new CurrentTimeFunction( this );
	}

	// arithmetic operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <N extends Number> Expression<N> neg(Expression<N> expression) {
		return new UnaryArithmeticOperation<N>(
				this,
				UnaryArithmeticOperation.Operation.UNARY_MINUS,
				expression
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> sum(Expression<? extends N> expression1, Expression<? extends N> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( expression1.getJavaType(), expression2.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.ADD,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> prod(Expression<? extends N> expression1, Expression<? extends N> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to prod() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( expression1.getJavaType(), expression2.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.MULTIPLY,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> diff(Expression<? extends N> expression1, Expression<? extends N> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( expression1.getJavaType(), expression2.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.SUBTRACT,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> sum(Expression<? extends N> expression, N n) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( expression.getJavaType(), n.getClass() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.ADD,
				expression,
				n
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> prod(Expression<? extends N> expression, N n) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to prod() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( expression.getJavaType(), n.getClass() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.MULTIPLY,
				expression,
				n
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> diff(Expression<? extends N> expression, N n) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( expression.getJavaType(), n.getClass() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.SUBTRACT,
				expression,
				n
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> sum(N n, Expression<? extends N> expression) {
		if ( expression == null || n == null ) {
			throw new IllegalArgumentException( "arguments to sum() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( n.getClass(), expression.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.ADD,
				n,
				expression
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> prod(N n, Expression<? extends N> expression) {
		if ( n == null || expression == null ) {
			throw new IllegalArgumentException( "arguments to prod() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( n.getClass(), expression.getJavaType() );

		return (BinaryArithmeticOperation<N>) new BinaryArithmeticOperation(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.MULTIPLY,
				n,
				expression
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <N extends Number> Expression<N> diff(N n, Expression<? extends N> expression) {
		if ( n == null || expression == null ) {
			throw new IllegalArgumentException( "arguments to diff() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( n.getClass(), expression.getJavaType() );

		return new BinaryArithmeticOperation<N>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.SUBTRACT,
				n,
				expression
		);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public Expression<Number> quot(Expression<? extends Number> expression1, Expression<? extends Number> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( expression1.getJavaType(), expression2.getJavaType(), true );

		return new BinaryArithmeticOperation<Number>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.DIVIDE,
				expression1,
				expression2
		);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public Expression<Number> quot(Expression<? extends Number> expression, Number number) {
		if ( expression == null || number == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( expression.getJavaType(), number.getClass(), true );

		return new BinaryArithmeticOperation<Number>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.DIVIDE,
				expression,
				number
		);
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public Expression<Number> quot(Number number, Expression<? extends Number> expression) {
		if ( expression == null || number == null ) {
			throw new IllegalArgumentException( "arguments to quot() cannot be null" );
		}

		final Class resultType = TypeHelper.determineResultType( number.getClass(), expression.getJavaType(), true );

		return new BinaryArithmeticOperation<Number>(
				this,
				resultType,
				BinaryArithmeticOperation.Operation.DIVIDE,
				number,
				expression
		);
	}

	@Override
	public Expression<Integer> mod(Expression<Integer> expression1, Expression<Integer> expression2) {
		if ( expression1 == null || expression2 == null ) {
			throw new IllegalArgumentException( "arguments to mod() cannot be null" );
		}

		return new BinaryArithmeticOperation<Integer>(
				this,
				Integer.class,
				BinaryArithmeticOperation.Operation.MOD,
				expression1,
				expression2
		);
	}

	@Override
	public Expression<Integer> mod(Expression<Integer> expression, Integer integer) {
		if ( expression == null || integer == null ) {
			throw new IllegalArgumentException( "arguments to mod() cannot be null" );
		}

		return new BinaryArithmeticOperation<Integer>(
				this,
				Integer.class,
				BinaryArithmeticOperation.Operation.MOD,
				expression,
				integer
		);
	}

	@Override
	public Expression<Integer> mod(Integer integer, Expression<Integer> expression) {
		if ( integer == null || expression == null ) {
			throw new IllegalArgumentException( "arguments to mod() cannot be null" );
		}

		return new BinaryArithmeticOperation<Integer>(
				this,
				Integer.class,
				BinaryArithmeticOperation.Operation.MOD,
				integer,
				expression
		);
	}


	// casting ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public ExpressionImplementor<Long> toLong(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asLong();
	}

	@Override
	public ExpressionImplementor<Integer> toInteger(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asInteger();
	}

	@Override
	public ExpressionImplementor<Float> toFloat(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asFloat();
	}

	@Override
	public ExpressionImplementor<Double> toDouble(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asDouble();
	}

	@Override
	public ExpressionImplementor<BigDecimal> toBigDecimal(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asBigDecimal();
	}

	@Override
	public ExpressionImplementor<BigInteger> toBigInteger(Expression<? extends Number> expression) {
		return ( (ExpressionImplementor<? extends Number>) expression ).asBigInteger();
	}

	@Override
	public ExpressionImplementor<String> toString(Expression<Character> characterExpression) {
		return ( (ExpressionImplementor<Character>) characterExpression ).asString();
	}

	// subqueries ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Predicate exists(Subquery<?> subquery) {
		return new ExistsPredicate( this, subquery );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> Expression<Y> all(Subquery<Y> subquery) {
		return new SubqueryComparisonModifierExpression<Y>(
				this,
				(Class<Y>) subquery.getJavaType(),
				subquery,
				SubqueryComparisonModifierExpression.Modifier.ALL
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> Expression<Y> some(Subquery<Y> subquery) {
		return new SubqueryComparisonModifierExpression<Y>(
				this,
				(Class<Y>) subquery.getJavaType(),
				subquery,
				SubqueryComparisonModifierExpression.Modifier.SOME
		);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <Y> Expression<Y> any(Subquery<Y> subquery) {
		return new SubqueryComparisonModifierExpression<Y>(
				this,
				(Class<Y>) subquery.getJavaType(),
				subquery,
				SubqueryComparisonModifierExpression.Modifier.ANY
		);
	}


	// miscellaneous expressions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <Y> Expression<Y> coalesce(Expression<? extends Y> exp1, Expression<? extends Y> exp2) {
		return coalesce( (Class<Y>) null, exp1, exp2 );
	}

	public <Y> Expression<Y> coalesce(Class<Y> type, Expression<? extends Y> exp1, Expression<? extends Y> exp2) {
		return new CoalesceExpression<Y>( this, type ).value( exp1 ).value( exp2 );
	}

	@Override
	public <Y> Expression<Y> coalesce(Expression<? extends Y> exp1, Y exp2) {
		return coalesce( (Class<Y>) null, exp1, exp2 );
	}

	public <Y> Expression<Y> coalesce(Class<Y> type, Expression<? extends Y> exp1, Y exp2) {
		return new CoalesceExpression<Y>( this, type ).value( exp1 ).value( exp2 );
	}

	@Override
	public <T> Coalesce<T> coalesce() {
		return coalesce( (Class<T>)null );
	}

	public <T> Coalesce<T> coalesce(Class<T> type) {
		return new CoalesceExpression<T>( this, type );
	}

	@Override
	public Expression<String> concat(Expression<String> string1, Expression<String> string2) {
		return new ConcatExpression( this, string1, string2 );
	}

	@Override
	public Expression<String> concat(Expression<String> string1, String string2) {
		return new ConcatExpression( this, string1, string2 );
	}

	@Override
	public Expression<String> concat(String string1, Expression<String> string2) {
		return new ConcatExpression( this, string1, string2 );
	}

	@Override
	public <Y> Expression<Y> nullif(Expression<Y> exp1, Expression<?> exp2) {
		return nullif( null, exp1, exp2 );
	}

	public <Y> Expression<Y> nullif(Class<Y> type, Expression<Y> exp1, Expression<?> exp2) {
		return new NullifExpression<Y>( this, type, exp1, exp2 );
	}

	@Override
	public <Y> Expression<Y> nullif(Expression<Y> exp1, Y exp2) {
		return nullif( null, exp1, exp2 );
	}

	public <Y> Expression<Y> nullif(Class<Y> type, Expression<Y> exp1, Y exp2) {
		return new NullifExpression<Y>( this, type, exp1, exp2 );
	}

	@Override
	public <C, R> SimpleCase<C, R> selectCase(Expression<? extends C> expression) {
		return selectCase( (Class<R>)null, expression );
	}

	public <C, R> SimpleCase<C, R> selectCase(Class<R> type, Expression<? extends C> expression) {
		return new SimpleCaseExpression<C, R>( this, type, expression );
	}

	@Override
	public <R> Case<R> selectCase() {
		return selectCase( (Class<R>)null );
	}

	public <R> Case<R> selectCase(Class<R> type) {
		return new SearchedCaseExpression<R>( this, type );
	}

}
