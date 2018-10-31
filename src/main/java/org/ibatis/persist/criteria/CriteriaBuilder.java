package org.ibatis.persist.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Used to construct criteria queries, compound selections, 
 * expressions, predicates, orderings.
 *
 * <p> Note that <code>Predicate</code> is used instead of <code>Expression&#060;Boolean&#062;</code> 
 * in this API in order to work around the fact that Java 
 * generics are not compatible with varags.
 *
 * @since iBatis Persistence 1.0
 */
public interface CriteriaBuilder {

    /**
     *  Create a <code>CriteriaQuery</code> object with the specified result 
     *  type.
     *  @param resultClass  type of the query result
     *  @return criteria query object
     */
    <T> CriteriaQuery<T> createQuery(Class<T> resultClass);

    // methods to construct queries for bulk updates and deletes:

    /**
     *  Create a <code>CriteriaUpdate</code> query object to perform a bulk update operation.
     *  @param targetEntity  target type for update operation
     *  @return the query object
     *  @since iBatis Persistence 1.0
     */
    <T> CriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity);

    /**
     *  Create a <code>CriteriaDelete</code> query object to perform a bulk delete operation.
     *  @param targetEntity  target type for delete operation
     *  @return the query object
     *  @since iBatis Persistence 1.0
     */
    <T> CriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity);

    //ordering:
	
    /**
     * Create an ordering by the ascending value of the expression.
     * @param x  expression used to define the ordering
     * @return ascending ordering corresponding to the expression
     */
    Order asc(Expression<?> x);

    /**
     * Create an ordering by the descending value of the expression.
     * @param x  expression used to define the ordering
     * @return descending ordering corresponding to the expression
     */
    Order desc(Expression<?> x);

	
    //aggregate functions:
	
    /**
     * Create an aggregate expression applying the avg operation.
     * @param x  expression representing input value to avg operation
     * @return avg expression
     */
    <N extends Number> Expression<Double> avg(Expression<N> x);

    /**
     * Create an aggregate expression applying the sum operation.
     * @param x  expression representing input value to sum operation
     * @return sum expression
     */
    <N extends Number> Expression<N> sum(Expression<N> x);

    /**
     * Create an aggregate expression applying the sum operation to an
     * Integer-valued expression, returning a Long result.
     * @param x  expression representing input value to sum operation
     * @return sum expression
     */
    Expression<Long> sumAsLong(Expression<Integer> x);

    /**
     * Create an aggregate expression applying the sum operation to a
     * Float-valued expression, returning a Double result.
     * @param x  expression representing input value to sum operation
     * @return sum expression
     */
    Expression<Double> sumAsDouble(Expression<Float> x);
    
    /**
     * Create an aggregate expression applying the numerical max 
     * operation.
     * @param x  expression representing input value to max operation
     * @return max expression
     */
    <N extends Number> Expression<N> max(Expression<N> x);
    
    /**
     * Create an aggregate expression applying the numerical min 
     * operation.
     * @param x  expression representing input value to min operation
     * @return min expression
     */
    <N extends Number> Expression<N> min(Expression<N> x);

    /**
     * Create an aggregate expression for finding the greatest of
     * the values (strings, dates, etc).
     * @param x  expression representing input value to greatest
     *           operation
     * @return greatest expression
     */
    <X extends Comparable<? super X>> Expression<X> greatest(Expression<X> x);
    
    /**
     * Create an aggregate expression for finding the least of
     * the values (strings, dates, etc).
     * @param x  expression representing input value to least
     *           operation
     * @return least expression
     */
    <X extends Comparable<? super X>> Expression<X> least(Expression<X> x);

    /**
     * Create an aggregate expression applying the count operation.
     * @param x  expression representing input value to count 
     *           operation
     * @return count expression
     */
    Expression<Long> count(Expression<?> x);

    /**
     * Create an aggregate expression applying the count distinct 
     * operation.
     * @param x  expression representing input value to 
     *        count distinct operation
     * @return count distinct expression
     */
    Expression<Long> countDistinct(Expression<?> x);

    //subqueries:
	
    /**
     * Create a predicate testing the existence of a subquery result.
     * @param subquery  subquery whose result is to be tested
     * @return exists predicate
     */
    Predicate exists(Subquery<?> subquery);
	
    /**
     * Create an all expression over the subquery results.
     * @param subquery  subquery
     * @return all expression
     */
    <Y> Expression<Y> all(Subquery<Y> subquery);
	
    /**
     * Create a some expression over the subquery results.
     * This expression is equivalent to an <code>any</code> expression.
     * @param subquery  subquery
     * @return some expression
     */
    <Y> Expression<Y> some(Subquery<Y> subquery);
	
    /**
     * Create an any expression over the subquery results. 
     * This expression is equivalent to a <code>some</code> expression.
     * @param subquery  subquery
     * @return any expression
     */
    <Y> Expression<Y> any(Subquery<Y> subquery);


    //boolean functions:
	
    /**
     * Create a conjunction of the given boolean expressions.
     * @param x  boolean expression
     * @param y  boolean expression
     * @return and predicate
     */
    Predicate and(Expression<Boolean> x, Expression<Boolean> y);
    
    /**
     * Create a conjunction of the given restriction predicates.
     * A conjunction of zero predicates is true.
     * @param restrictions  zero or more restriction predicates
     * @return and predicate
     */
    Predicate and(Predicate... restrictions);

    /**
     * Create a disjunction of the given boolean expressions.
     * @param x  boolean expression
     * @param y  boolean expression
     * @return or predicate
     */
    Predicate or(Expression<Boolean> x, Expression<Boolean> y);

    /**
     * Create a disjunction of the given restriction predicates.
     * A disjunction of zero predicates is false.
     * @param restrictions  zero or more restriction predicates
     * @return or predicate
     */
    Predicate or(Predicate... restrictions);

    /**
     * Create a negation of the given restriction. 
     * @param restriction  restriction expression
     * @return not predicate
     */
    Predicate not(Expression<Boolean> restriction);
	
    /**
     * Create a conjunction (with zero conjuncts).
     * A conjunction with zero conjuncts is true.
     * @return and predicate
     */
    Predicate conjunction();

    /**
     * Create a disjunction (with zero disjuncts).
     * A disjunction with zero disjuncts is false.
     * @return or predicate
     */
    Predicate disjunction();

	
    //turn Expression<Boolean> into a Predicate
    //useful for use with varargs methods

    /**
     * Create a predicate testing for a true value.
     * @param x  expression to be tested
     * @return predicate
     */
    Predicate isTrue(Expression<Boolean> x);

    /**
     * Create a predicate testing for a false value.
     * @param x  expression to be tested
     * @return predicate
     */
    Predicate isFalse(Expression<Boolean> x);

	
    //null tests:

    /**
     * Create a predicate to test whether the expression is null.
     * @param x expression
     * @return is-null predicate
     */
    Predicate isNull(Expression<?> x);

    /**
     * Create a predicate to test whether the expression is not null.
     * @param x expression
     * @return is-not-null predicate
     */
    Predicate isNotNull(Expression<?> x);

    //equality:
	
    /**
     * Create a predicate for testing the arguments for equality.
     * @param x  expression
     * @param y  expression
     * @return equality predicate
     */
    Predicate equal(Expression<?> x, Expression<?> y);
	
    /**
     * Create a predicate for testing the arguments for equality.
     * @param x  expression
     * @param y  object
     * @return equality predicate
     */
    Predicate equal(Expression<?> x, Object y);

    /**
     * Create a predicate for testing the arguments for inequality.
     * @param x  expression
     * @param y  expression
     * @return inequality predicate
     */
    Predicate notEqual(Expression<?> x, Expression<?> y);
	
    /**
     * Create a predicate for testing the arguments for inequality.
     * @param x  expression
     * @param y  object
     * @return inequality predicate
     */
    Predicate notEqual(Expression<?> x, Object y);

	
    //comparisons for generic (non-numeric) operands:

    /**
     * Create a predicate for testing whether the first argument is 
     * greater than the second.
     * @param x  expression
     * @param y  expression
     * @return greater-than predicate
     */
    <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y);
	
    /**
     * Create a predicate for testing whether the first argument is 
     * greater than the second.
     * @param x  expression
     * @param y  value
     * @return greater-than predicate
     */
    <Y extends Comparable<? super Y>> Predicate greaterThan(Expression<? extends Y> x, Y y);
    
    /**
     * Create a predicate for testing whether the first argument is 
     * greater than or equal to the second.
     * @param x  expression
     * @param y  expression
     * @return greater-than-or-equal predicate
     */
    <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y);

    /**
     * Create a predicate for testing whether the first argument is 
     * greater than or equal to the second.
     * @param x  expression
     * @param y  value
     * @return greater-than-or-equal predicate
     */
    <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y);

    /**
     * Create a predicate for testing whether the first argument is 
     * less than the second.
     * @param x  expression
     * @param y  expression
     * @return less-than predicate
     */
    <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Expression<? extends Y> y);

    /**
     * Create a predicate for testing whether the first argument is 
     * less than the second.
     * @param x  expression
     * @param y  value
     * @return less-than predicate
     */
    <Y extends Comparable<? super Y>> Predicate lessThan(Expression<? extends Y> x, Y y);
	
    /**
     * Create a predicate for testing whether the first argument is 
     * less than or equal to the second.
     * @param x  expression
     * @param y  expression
     * @return less-than-or-equal predicate
     */
    <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y);

    /**
     * Create a predicate for testing whether the first argument is 
     * less than or equal to the second.
     * @param x  expression
     * @param y  value
     * @return less-than-or-equal predicate
     */
    <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(Expression<? extends Y> x, Y y);

    /**
     * Create a predicate for testing whether the first argument is 
     * between the second and third arguments in value.
     * @param v  expression 
     * @param x  expression
     * @param y  expression
     * @return between predicate
     */
    <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> v, Expression<? extends Y> x, Expression<? extends Y> y);

    /**
     * Create a predicate for testing whether the first argument is 
     * between the second and third arguments in value.
     * @param v  expression 
     * @param x  value
     * @param y  value
     * @return between predicate
     */
    <Y extends Comparable<? super Y>> Predicate between(Expression<? extends Y> v, Y x, Y y);
	

    //comparisons for numeric operands:
	
    /**
     * Create a predicate for testing whether the first argument is 
     * greater than the second.
     * @param x  expression
     * @param y  expression
     * @return greater-than predicate
     */
    Predicate gt(Expression<? extends Number> x, Expression<? extends Number> y);

    /**
     * Create a predicate for testing whether the first argument is 
     * greater than the second.
     * @param x  expression
     * @param y  value
     * @return greater-than predicate
     */
    Predicate gt(Expression<? extends Number> x, Number y);

    /**
     * Create a predicate for testing whether the first argument is 
     * greater than or equal to the second.
     * @param x  expression
     * @param y  expression
     * @return greater-than-or-equal predicate
     */
    Predicate ge(Expression<? extends Number> x, Expression<? extends Number> y);

    /**
     * Create a predicate for testing whether the first argument is 
     * greater than or equal to the second.
     * @param x  expression
     * @param y  value
     * @return greater-than-or-equal predicate
     */	
    Predicate ge(Expression<? extends Number> x, Number y);

    /**
     * Create a predicate for testing whether the first argument is 
     * less than the second.
     * @param x  expression
     * @param y  expression
     * @return less-than predicate
     */
    Predicate lt(Expression<? extends Number> x, Expression<? extends Number> y);

    /**
     * Create a predicate for testing whether the first argument is 
     * less than the second.
     * @param x  expression
     * @param y  value
     * @return less-than predicate
     */
    Predicate lt(Expression<? extends Number> x, Number y);

    /**
     * Create a predicate for testing whether the first argument is 
     * less than or equal to the second.
     * @param x  expression
     * @param y  expression
     * @return less-than-or-equal predicate
     */
    Predicate le(Expression<? extends Number> x, Expression<? extends Number> y);

    /**
     * Create a predicate for testing whether the first argument is 
     * less than or equal to the second.
     * @param x  expression
     * @param y  value
     * @return less-than-or-equal predicate
     */
    Predicate le(Expression<? extends Number> x, Number y);
	

    //numerical operations:
	
    /**
     * Create an expression that returns the arithmetic negation
     * of its argument.
     * @param x expression
     * @return arithmetic negation
     */
    <N extends Number> Expression<N> neg(Expression<N> x);

    /**
     * Create an expression that returns the absolute value
     * of its argument.
     * @param x expression
     * @return absolute value
     */
    <N extends Number> Expression<N> abs(Expression<N> x);
	
    /**
     * Create an expression that returns the sum
     * of its arguments.
     * @param x expression
     * @param y expression
     * @return sum
     */
    <N extends Number> Expression<N> sum(Expression<? extends N> x, Expression<? extends N> y);
	
    /**
     * Create an expression that returns the sum
     * of its arguments.
     * @param x expression
     * @param y value
     * @return sum
     */
    <N extends Number> Expression<N> sum(Expression<? extends N> x, N y);

    /**
     * Create an expression that returns the sum
     * of its arguments.
     * @param x value
     * @param y expression
     * @return sum
     */
    <N extends Number> Expression<N> sum(N x, Expression<? extends N> y);

    /**
     * Create an expression that returns the product
     * of its arguments.
     * @param x expression
     * @param y expression
     * @return product
     */
    <N extends Number> Expression<N> prod(Expression<? extends N> x, Expression<? extends N> y);

    /**
     * Create an expression that returns the product
     * of its arguments.
     * @param x expression
     * @param y value
     * @return product
     */
    <N extends Number> Expression<N> prod(Expression<? extends N> x, N y);

    /**
     * Create an expression that returns the product
     * of its arguments.
     * @param x value
     * @param y expression
     * @return product
     */
    <N extends Number> Expression<N> prod(N x, Expression<? extends N> y);

    /**
     * Create an expression that returns the difference
     * between its arguments.
     * @param x expression
     * @param y expression
     * @return difference
     */
    <N extends Number> Expression<N> diff(Expression<? extends N> x, Expression<? extends N> y);

    /**
     * Create an expression that returns the difference
     * between its arguments.
     * @param x expression
     * @param y value
     * @return difference
     */
    <N extends Number> Expression<N> diff(Expression<? extends N> x, N y);

    /**
     * Create an expression that returns the difference
     * between its arguments.
     * @param x value
     * @param y expression
     * @return difference
     */
    <N extends Number> Expression<N> diff(N x, Expression<? extends N> y);
	
    /**
     * Create an expression that returns the quotient
     * of its arguments.
     * @param x expression
     * @param y expression
     * @return quotient
     */
    Expression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y);

    /**
     * Create an expression that returns the quotient
     * of its arguments.
     * @param x expression
     * @param y value
     * @return quotient
     */
    Expression<Number> quot(Expression<? extends Number> x, Number y);

    /**
     * Create an expression that returns the quotient
     * of its arguments.
     * @param x value
     * @param y expression
     * @return quotient
     */
    Expression<Number> quot(Number x, Expression<? extends Number> y);
	
    /**
     * Create an expression that returns the modulus
     * of its arguments.
     * @param x expression
     * @param y expression
     * @return modulus
     */
    Expression<Integer> mod(Expression<Integer> x, Expression<Integer> y);
	
    /**
     * Create an expression that returns the modulus
     * of its arguments.
     * @param x expression
     * @param y value
     * @return modulus
     */
    Expression<Integer> mod(Expression<Integer> x, Integer y);

    /**
     * Create an expression that returns the modulus
     * of its arguments.
     * @param x value
     * @param y expression
     * @return modulus
     */
    Expression<Integer> mod(Integer x, Expression<Integer> y);

    /**
     * Create an expression that returns the square root
     * of its argument.
     * @param x expression
     * @return square root
     */	
    Expression<Double> sqrt(Expression<? extends Number> x);

	
    //typecasts:
    
    /**
     * Typecast.  Returns same expression object.
     * @param number  numeric expression
     * @return Expression&#060;Long&#062;
     */
    Expression<Long> toLong(Expression<? extends Number> number);

    /**
     * Typecast.  Returns same expression object.
     * @param number  numeric expression
     * @return Expression&#060;Integer&#062;
     */
    Expression<Integer> toInteger(Expression<? extends Number> number);

    /**
     * Typecast. Returns same expression object.
     * @param number  numeric expression
     * @return Expression&#060;Float&#062;
     */
    Expression<Float> toFloat(Expression<? extends Number> number);

    /**
     * Typecast.  Returns same expression object.
     * @param number  numeric expression
     * @return Expression&#060;Double&#062;
     */
    Expression<Double> toDouble(Expression<? extends Number> number);

    /**
     * Typecast.  Returns same expression object.
     * @param number  numeric expression
     * @return Expression&#060;BigDecimal&#062;
     */
    Expression<BigDecimal> toBigDecimal(Expression<? extends Number> number);

    /**
     * Typecast.  Returns same expression object.
     * @param number  numeric expression
     * @return Expression&#060;BigInteger&#062;
     */
    Expression<BigInteger> toBigInteger(Expression<? extends Number> number);
	
    /**
     * Typecast.  Returns same expression object.
     * @param character expression
     * @return Expression&#060;String&#062;
     */
    Expression<String> toString(Expression<Character> character);

	
    //literals:

    /**
     * Create an expression for a literal.
     * @param value  value represented by the expression
     * @return expression literal
     * @throws IllegalArgumentException if value is null
     */
    <T> Expression<T> literal(T value);

    /**
     * Create an expression for a null literal with the given type.
     * @param resultClass  type of the null literal
     * @return null expression literal
     */
    <T> Expression<T> nullLiteral(Class<T> resultClass);

    //parameters:

    /**
     * Create a parameter expression.
     * @param paramClass parameter class
     * @return parameter expression
     */
    <T> ParameterExpression<T> parameter(Class<T> paramClass);

    /**
     * Create a parameter expression with the given name.
     * @param paramClass parameter class
     * @param name  name that can be used to refer to 
     *              the parameter
     * @return parameter expression
     */
    <T> ParameterExpression<T> parameter(Class<T> paramClass, String name);

    //string functions:
	
    /**
     * Create a predicate for testing whether the expression
     * satisfies the given pattern.
     * @param x  string expression
     * @param pattern  string expression
     * @return like predicate
     */
    Predicate like(Expression<String> x, Expression<String> pattern);
	
    /**
     * Create a predicate for testing whether the expression
     * satisfies the given pattern.
     * @param x  string expression
     * @param pattern  string 
     * @return like predicate
     */
    Predicate like(Expression<String> x, String pattern);
	
    /**
     * Create a predicate for testing whether the expression
     * satisfies the given pattern.
     * @param x  string expression
     * @param pattern  string expression
     * @param escapeChar  escape character expression
     * @return like predicate
     */
    Predicate like(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);
	
    /**
     * Create a predicate for testing whether the expression
     * satisfies the given pattern.
     * @param x  string expression
     * @param pattern  string expression
     * @param escapeChar  escape character
     * @return like predicate
     */
    Predicate like(Expression<String> x, Expression<String> pattern, char escapeChar);
	
    /**
     * Create a predicate for testing whether the expression
     * satisfies the given pattern.
     * @param x  string expression
     * @param pattern  string 
     * @param escapeChar  escape character expression
     * @return like predicate
     */
    Predicate like(Expression<String> x, String pattern, Expression<Character> escapeChar);

    /**
     * Create a predicate for testing whether the expression
     * satisfies the given pattern.
     * @param x  string expression
     * @param pattern  string 
     * @param escapeChar  escape character
     * @return like predicate
     */
    Predicate like(Expression<String> x, String pattern, char escapeChar);
	
    /**
     * Create a predicate for testing whether the expression
     * does not satisfy the given pattern.
     * @param x  string expression
     * @param pattern  string expression
     * @return not-like predicate
     */
    Predicate notLike(Expression<String> x, Expression<String> pattern);
	
    /**
     * Create a predicate for testing whether the expression
     * does not satisfy the given pattern.
     * @param x  string expression
     * @param pattern  string 
     * @return not-like predicate
     */
    Predicate notLike(Expression<String> x, String pattern);

    /**
     * Create a predicate for testing whether the expression
     * does not satisfy the given pattern.
     * @param x  string expression
     * @param pattern  string expression
     * @param escapeChar  escape character expression
     * @return not-like predicate
     */
    Predicate notLike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

    /**
     * Create a predicate for testing whether the expression
     * does not satisfy the given pattern.
     * @param x  string expression
     * @param pattern  string expression
     * @param escapeChar  escape character
     * @return not-like predicate
     */
    Predicate notLike(Expression<String> x, Expression<String> pattern, char escapeChar);

    /**
     * Create a predicate for testing whether the expression
     * does not satisfy the given pattern.
     * @param x  string expression
     * @param pattern  string 
     * @param escapeChar  escape character expression
     * @return not-like predicate
     */
    Predicate notLike(Expression<String> x, String pattern, Expression<Character> escapeChar);
	
   /**
     * Create a predicate for testing whether the expression
     * does not satisfy the given pattern.
     * @param x  string expression
     * @param pattern  string 
     * @param escapeChar  escape character
     * @return not-like predicate
     */
    Predicate notLike(Expression<String> x, String pattern, char escapeChar);

    /**
     *  Create an expression for string concatenation.
     *  @param x  string expression
     *  @param y  string expression
     *  @return expression corresponding to concatenation
     */
    Expression<String> concat(Expression<String> x, Expression<String> y);
	
    /**
     *  Create an expression for string concatenation.
     *  @param x  string expression
     *  @param y  string 
     *  @return expression corresponding to concatenation
     */
    Expression<String> concat(Expression<String> x, String y);

    /**
     *  Create an expression for string concatenation.
     *  @param x  string 
     *  @param y  string expression
     *  @return expression corresponding to concatenation
     */
    Expression<String> concat(String x, Expression<String> y);

    // Date/time/timestamp functions:

    /**
     *  Create expression to return current date.
     *  @return expression for current date
     */
    Expression<java.sql.Date> currentDate();

    /**
     *  Create expression to return current timestamp.
     *  @return expression for current timestamp
     */	
    Expression<java.sql.Timestamp> currentTimestamp();

    /**
     *  Create expression to return current time.
     *  @return expression for current time
     */	
    Expression<java.sql.Time> currentTime();
	

    //in builders:
	
    /**
     *  Interface used to build in predicates.
     */
    public static interface In<T> extends Predicate {

         /**
          * Return the expression to be tested against the
          * list of values.
          * @return expression
          */
         Expression<T> getExpression();
	
         /**
          *  Add to list of values to be tested against.
          *  @param value value
          *  @return in predicate
          */
         In<T> value(T value);

         /**
          *  Add to list of values to be tested against.
          *  @param value expression
          *  @return in predicate
          */
         In<T> value(Expression<? extends T> value);
     }
	
    /**
     *  Create predicate to test whether given expression
     *  is contained in a list of values.
     *  @param  expression to be tested against list of values
     *  @return  in predicate
     */
    <T> In<T> in(Expression<? extends T> expression);
	

    // coalesce, nullif:

    /**
     * Create an expression that returns null if all its arguments
     * evaluate to null, and the value of the first non-null argument
     * otherwise.
     * @param x expression
     * @param y expression
     * @return coalesce expression
     */
    <Y> Expression<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y);

    /**
     * Create an expression that returns null if all its arguments
     * evaluate to null, and the value of the first non-null argument
     * otherwise.
     * @param x expression
     * @param y value
     * @return coalesce expression
     */
    <Y> Expression<Y> coalesce(Expression<? extends Y> x, Y y);
    
    /**
     * Create an expression that tests whether its argument are
     * equal, returning null if they are and the value of the
     * first expression if they are not.
     * @param x expression
     * @param y expression
     * @return nullif expression
     */
    <Y> Expression<Y> nullif(Expression<Y> x, Expression<?> y);

    /**
     * Create an expression that tests whether its argument are
     * equal, returning null if they are and the value of the
     * first expression if they are not.
     * @param x expression
     * @param y value
     * @return nullif expression 
     */
    <Y> Expression<Y> nullif(Expression<Y> x, Y y);


    // coalesce builder:

    /**
     *  Interface used to build coalesce expressions.  
     *   
     * A coalesce expression is equivalent to a case expression
     * that returns null if all its arguments evaluate to null,
     * and the value of its first non-null argument otherwise.
     */
    public static interface Coalesce<T> extends Expression<T> {

         /**
          * Add an argument to the coalesce expression.
          * @param value  value
          * @return coalesce expression
          */
         Coalesce<T> value(T value);

         /**
          * Add an argument to the coalesce expression.
          * @param value expression
          * @return coalesce expression
          */
         Coalesce<T> value(Expression<? extends T> value);
	}
	
    /**
     * Create a coalesce expression.
     * @return coalesce expression
     */
    <T> Coalesce<T> coalesce();


    //case builders:

    /**
     *  Interface used to build simple case expressions.
     *  Case conditions are evaluated in the order in which
     *  they are specified.
     */
    public static interface SimpleCase<C,R> extends Expression<R> {

		/**
		 * Return the expression to be tested against the
		 * conditions.
		 * @return expression
		 */
		Expression<C> getExpression();

		/**
		 * Add a when/then clause to the case expression.
		 * @param condition  "when" condition
		 * @param result  "then" result value
		 * @return simple case expression
		 */
		SimpleCase<C, R> when(C condition, R result);

		/**
		 * Add a when/then clause to the case expression.
		 * @param condition  "when" condition
		 * @param result  "then" result expression
		 * @return simple case expression
		 */
		SimpleCase<C, R> when(C condition, Expression<? extends R> result);

		/**
		 * Add an "else" clause to the case expression.
		 * @param result  "else" result
		 * @return expression
		 */
		Expression<R> otherwise(R result);

		/**
		 * Add an "else" clause to the case expression.
		 * @param result  "else" result expression
		 * @return expression
		 */
		Expression<R> otherwise(Expression<? extends R> result);
	}
	
    /**
     *  Create a simple case expression.
     *  @param expression  to be tested against the case conditions
     *  @return simple case expression
     */
    <C, R> SimpleCase<C,R> selectCase(Expression<? extends C> expression);


    /**
     *  Interface used to build general case expressions.
     *  Case conditions are evaluated in the order in which
     *  they are specified.
     */
    public static interface Case<R> extends Expression<R> {

		/**
		 * Add a when/then clause to the case expression.
		 * @param condition  "when" condition
		 * @param result  "then" result value
		 * @return general case expression
		 */
		Case<R> when(Expression<Boolean> condition, R result);

		/**
		 * Add a when/then clause to the case expression.
		 * @param condition  "when" condition
		 * @param result  "then" result expression
		 * @return general case expression
		 */
		Case<R> when(Expression<Boolean> condition, Expression<? extends R> result);

		/**
		 * Add an "else" clause to the case expression.
		 * @param result  "else" result
		 * @return expression
		 */
		Expression<R> otherwise(R result);

		/**
		 * Add an "else" clause to the case expression.
		 * @param result  "else" result expression
		 * @return expression
		 */
		Expression<R> otherwise(Expression<? extends R> result);
	}
	
    /**
     *  Create a general case expression.
     *  @return general case expression
     */
    <R> Case<R> selectCase();

    /**
     * Create an expression for the execution of a database
     * function.
     * @param name  function name
     * @param type  expected result type
     * @param args  function arguments
     * @return expression
     */
   <T> Expression<T> function(String name, Class<T> type, Expression<?>... args);

}




