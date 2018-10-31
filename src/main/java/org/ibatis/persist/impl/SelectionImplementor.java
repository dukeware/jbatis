package org.ibatis.persist.impl;

import java.util.List;

import org.ibatis.persist.criteria.Selection;

import com.ibatis.sqlmap.engine.type.TypeHandler;

public interface SelectionImplementor<X> extends Selection<X>, Renderable {

    void setJavaType(Class<?> targetType);
    
    public List<TypeHandler<?>> getValueHandlers();

    public TypeHandler<X> getValueHandler();
}
