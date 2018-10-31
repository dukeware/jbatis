package org.ibatis.persist.criteria;

import java.util.Set;

import org.ibatis.persist.Parameter;

/**
 * Parameterized
 * 
 * @since iBatis Persistence 1.0
 */
public interface Parameterized<E> {
    
    /**
     * Bind the value of a <code>Parameter</code> object.
     * 
     * @param param
     *            parameter object
     * @param value
     *            parameter value
     * @return the same query instance
     * @since iBatis Persistence 1.0
     */
    <R> E setParameter(Parameter<R> param, R value);

    /**
     * Bind an argument value to a named parameter.
     * 
     * @param name
     *            parameter name
     * @param value
     *            parameter value
     * @return the same query instance
     */
    <R> E setParameter(String name, R value);

    /**
     * Bind an argument value to a positional parameter.
     * 
     * @param position
     *            position
     * @param value
     *            parameter value
     * @return the same query instance
     */
    <R> E setParameter(int position, R value);

    /**
     * Get the parameter objects corresponding to the declared parameters of the query. Returns empty set if the query
     * has no parameters. This method is not required to be supported for native queries.
     * 
     * @return set of the parameter objects
     */
    Set<? extends Parameter<?>> getParameters();

    /**
     * Get the parameter object corresponding to the declared parameter of the given name. This method is not required
     * to be supported for native queries.
     * 
     * @param name
     *            parameter name
     * @return parameter object
     */
    Parameter<?> getParameter(String name);

    /**
     * Get the parameter object corresponding to the declared parameter of the given name and type. This method is
     * required to be supported for criteria queries only.
     * 
     * @param name
     *            parameter name
     * @param type
     *            type
     * @return parameter object
     */
    <R> Parameter<R> getParameter(String name, Class<R> type);

    /**
     * Return a boolean indicating whether a value has been bound to the parameter.
     * 
     * @param param
     *            parameter object
     * @return boolean indicating whether parameter has been bound
     */
    boolean isBound(Parameter<?> param);

    /**
     * Return the input value bound to the parameter. (Note that OUT parameters are unbound.)
     * 
     * @param param
     *            parameter object
     * @return parameter value
     */
    <R> R getParameterValue(Parameter<R> param);

    /**
     * Return the input value bound to the named parameter. (Note that OUT parameters are unbound.)
     * 
     * @param name
     *            parameter name
     * @return parameter value
     */
    <R> R getParameterValue(String name);

}
