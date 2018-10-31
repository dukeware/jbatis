package org.ibatis.persist;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that the class is an entity. This annotation is applied to the entity class.
 * 
 * @since iBatis Persistence 1.0
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface Entity {

    /**
     * (Optional) The entity name. Defaults to the unqualified name of the entity class. This name is used to refer to
     * the entity in queries.
     */
    String name() default "";

    /**
     * (Optional) The entity namespace. Defaults to the qualified package name of the entity class.
     */
    String namespace() default "";
}
