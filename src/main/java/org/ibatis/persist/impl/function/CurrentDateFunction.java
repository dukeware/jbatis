package org.ibatis.persist.impl.function;

import java.sql.Date;

import org.ibatis.persist.impl.CriteriaBuilderImpl;

/**
 * Models the ANSI SQL <tt>CURRENT_DATE</tt> function.
 */
public class CurrentDateFunction
		extends BasicFunctionExpression<Date> {
	public static final String NAME = "current_date";

	public CurrentDateFunction(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder, Date.class, NAME );
	}
}
