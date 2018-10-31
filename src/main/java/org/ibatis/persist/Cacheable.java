package org.ibatis.persist;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specifies whether an entity should be cached. The value of the <code>Cacheable</code> annotation is inherited by
 * subclasses; it can be overridden by specifying <code>Cacheable</code> on a subclass.
 * 
 * <p>
 * <code>Cacheable(false)</code> means that the entity and its state must not be cached by the provider.
 * 
 * @since iBatis Persistence 1.0
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface Cacheable {

    /**
     * (Optional) Whether or not the entity should be cached.
     */
    boolean value() default true;

    /**
     * (Optional) The cache type such as MEMORY, OSCACHE, EHCACHE or others.
     */
    String type() default "";

    /**
     * (Optional)
     */
    int minutes() default 600;
    
    /**
     * (Optional) The cache roots
     * @since 1.1
     */
    Class<?>[] roots() default {};
}
