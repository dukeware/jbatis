package org.ibatis.persist;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the table for the annotated entity.
 * <p>
 * If no <code>Table</code> annotation is specified for an entity class, the default values apply.
 * @since iBatis Persistence 1.0
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Table {

    /**
     * (Optional) The name of the table.
     * <p>
     * Defaults to the entity name.
     */
    String name() default "";

    /**
     * (Optional) The schema of the table.
     * <p>
     * Defaults to the default schema for user.
     */
    String schema() default "";

}
