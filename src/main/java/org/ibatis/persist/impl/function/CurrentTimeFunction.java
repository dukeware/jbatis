package org.ibatis.persist.impl.function;

import java.sql.Time;

import org.ibatis.persist.impl.CriteriaBuilderImpl;

/**
 * Models the ANSI SQL <tt>CURRENT_TIME</tt> function.
 */
public class CurrentTimeFunction
		extends BasicFunctionExpression<Time> {
	public static final String NAME = "current_time";

	public CurrentTimeFunction(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder, Time.class, NAME );
	}
}
