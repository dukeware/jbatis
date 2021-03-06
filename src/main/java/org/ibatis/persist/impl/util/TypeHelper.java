package org.ibatis.persist.impl.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class TypeHelper {
	private TypeHelper() {
	}

	/**
	 * Determine the appropriate runtime result type for a numeric expression according to
	 * section "6.5.7.1 Result Types of Expressions" of the JPA spec.
	 * <p/>
	 * Note that it is expected that the caveats about quotient handling have already been handled.
	 *
	 * @param types The argument/expression types
	 *
	 * @return The appropriate numeric result type.
	 */
    @SuppressWarnings("unchecked")
	public static Class<? extends Number> determineResultType(Class<? extends Number>... types) {
		Class<? extends Number> result = Number.class;

		for ( Class<? extends Number> type : types ) {
			if ( Double.class.equals( type ) ) {
				result = Double.class;
			}
			else if ( Float.class.equals( type ) ) {
				result = Float.class;
			}
			else if ( BigDecimal.class.equals( type ) ) {
				result = BigDecimal.class;
			}
			else if ( BigInteger.class.equals( type ) ) {
				result = BigInteger.class;
			}
			else if ( Long.class.equals( type ) ) {
				result = Long.class;
			}
			else if ( isIntegralType( type ) ) {
				result = Integer.class;
			}
		}

		return result;
	}

	private static boolean isIntegralType(Class<? extends Number> type) {
		return Integer.class.equals( type ) ||
				Short.class.equals( type );

	}

    @SuppressWarnings("unchecked")
    public static Class<? extends Number> determineResultType(Class<? extends Number> argument1Type,
        Class<? extends Number> argument2Type, boolean isQuotientOperation) {
        if (isQuotientOperation) {
            return Number.class;
        }
        return determineResultType(argument1Type, argument2Type);
    }
}
