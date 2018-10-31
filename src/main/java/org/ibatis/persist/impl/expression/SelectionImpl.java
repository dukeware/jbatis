package org.ibatis.persist.impl.expression;

import java.util.Collections;
import java.util.List;

import org.ibatis.persist.criteria.Selection;
import org.ibatis.persist.impl.AbstractNode;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.SelectionImplementor;

import com.ibatis.sqlmap.engine.type.TypeHandler;

@SuppressWarnings("unchecked")
public abstract class SelectionImpl<X> extends AbstractNode
		implements SelectionImplementor<X> {
	public SelectionImpl(CriteriaBuilderImpl criteriaBuilder, Class<X> javaType) {
		super( criteriaBuilder );
        this.originalJavaType = javaType;
        this.javaType = javaType;
	}

	public Selection<X> alias(String alias) {
		setAlias( alias );
		return this;
	}

	public boolean isCompoundSelection() {
		return false;
	}

    public List<TypeHandler<?>> getValueHandlers() {
        TypeHandler<X> hander = getValueHandler();
        if (hander == null) {
            return null;
        }
        List<?> list = Collections.singletonList(hander);
        return (List<TypeHandler<?>>) list;
    }

	public List<Selection<?>> getCompoundSelectionItems() {
		throw new IllegalStateException( "Not a compound selection" );
	}

    final Class originalJavaType;
    private Class<X> javaType;
    private String alias;
    private TypeHandler<X> valueHandler;

    @Override
    public Class<X> getJavaType() {
        return javaType;
    }

    public void setJavaType(Class targetType) {
        this.javaType = targetType;
        this.valueHandler = (TypeHandler<X>) criteriaBuilder().getEntityManager().getDelegate().getTypeHandlerFactory().getTypeHandler(javaType);
    }

    protected void forceConversion(TypeHandler<X> valueHandler) {
        this.valueHandler = valueHandler;
    }

    @Override
    public TypeHandler<X> getValueHandler() {
        return valueHandler;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    /**
     * Protected access to define the alias.
     *
     * @param alias The alias to use.
     */
    protected void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public void renderFrom(RenderingContext rc) {
        throw new IllegalStateException( "Selection cannot occur in from clause" );
    }

}
