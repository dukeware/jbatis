package org.ibatis.persist;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the mapped column for a persistent property or field. If no <code>Column</code> annotation is specified,
 * the default values apply.
 *
 * @since iBatis Persistence 1.0
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface Column {

    /**
     * (Optional) The name of the column. Defaults to the property or field name.
     */
    String name() default "";

    /**
     * (Optional) The name of the table that contains the column. If absent the column is assumed to be in the primary
     * table.
     */
    String table() default "";
}
