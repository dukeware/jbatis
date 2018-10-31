package org.ibatis.persist.impl.function;

import java.sql.Timestamp;

import org.ibatis.persist.impl.CriteriaBuilderImpl;

/**
 * Models the ANSI SQL <tt>CURRENT_TIMESTAMP</tt> function.
 */
public class CurrentTimestampFunction
		extends BasicFunctionExpression<Timestamp> {
	public static final String NAME = "current_timestamp";

	public CurrentTimestampFunction(CriteriaBuilderImpl criteriaBuilder) {
		super( criteriaBuilder, Timestamp.class, NAME );
	}
}
