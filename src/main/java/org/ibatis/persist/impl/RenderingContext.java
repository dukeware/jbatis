package org.ibatis.persist.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.ibatis.persist.criteria.ParameterExpression;

/**
 * Used to provide a context and services to the rendering.
 */
public class RenderingContext {

    Stack<Object> stack = new Stack<Object>();
    private int aliasCount = 1;
    ParameterInfo<?>[] parameterInfos;
    Set<ParameterExpression<?>> parameterExpressions;
    boolean query;
    
    public RenderingContext() {
        query = true;
    }

    public boolean isQuery() {
        return query;
    }
    
    public void setQuery(boolean query) {
        this.query = query;
    }
    
    public String generateAlias() {
        return "_t" + aliasCount++;
    }
    public RenderingContext append(String str) {
        StringBuilder buf = buffer();
        buf.append(str);
        return this;
    }

    synchronized StringBuilder buffer() {
        if (stack.size() > 0) {
            Object top = stack.peek();
            if (top instanceof StringBuilder) {
                return (StringBuilder) top;
            }
        }
        stack.push(new StringBuilder());
        return (StringBuilder) stack.peek();
    }

    public synchronized RenderingContext append(ParameterInfo<?> parameter) {
        stack.push(parameter);
        return this;
    }

    public RenderingContext append(char c) {
        StringBuilder buf = buffer();
        buf.append(c);
        return this;
    }

    public String getSql() {
        // System.out.println(this);
        StringBuilder buf = new StringBuilder();
        for (Object o : stack) {
            if (o instanceof StringBuilder) {
                buf.append(o.toString());
            } else {
                buf.append("?");
            }
        }
        return buf.toString();
    }

    public ParameterInfo<?>[] getParameterInfos() {
        if (parameterInfos == null) {
            List<ParameterInfo<?>> list = new ArrayList<ParameterInfo<?>>();
            for (Object o : stack) {
                if (o instanceof ParameterInfo<?>) {
                    list.add((ParameterInfo<?>) o);
                }
            }
            parameterInfos = list.toArray(new ParameterInfo<?>[list.size()]);
        }
        return parameterInfos;
    }

    public Set<ParameterExpression<?>> getParameters() {
        if (parameterExpressions == null) {
            parameterExpressions = new HashSet<ParameterExpression<?>>();
            for (Object o : stack) {
                if (o instanceof ParameterExpression<?>) {
                    parameterExpressions.add((ParameterExpression<?>) o);
                }
            }
        }
        return Collections.unmodifiableSet(parameterExpressions);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Object o : stack) {
            buf.append(o.toString());
        }
        return buf.toString();
    }

}
