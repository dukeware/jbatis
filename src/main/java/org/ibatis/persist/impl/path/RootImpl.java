package org.ibatis.persist.impl.path;

import org.ibatis.persist.criteria.Path;
import org.ibatis.persist.criteria.Root;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.PathSource;
import org.ibatis.persist.impl.QueryStructure;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.util.GetterInterceptor;
import org.ibatis.persist.meta.Attribute;
import org.ibatis.persist.meta.EntityType;

@SuppressWarnings("unchecked")
public class RootImpl<X> extends AbstractFromImpl implements Root<X>, PathSource {
	private final EntityType<X> entityType;
	String alias;

	public RootImpl(CriteriaBuilderImpl criteriaBuilder, EntityType<X> entityType, QueryStructure queryStructure) {
        super( criteriaBuilder, queryStructure);
        this.entityType = entityType;
	}

	public EntityType<X> getEntityType() {
		return entityType;
	}

	public EntityType<X> getModel() {
		return getEntityType();
	}

	@Override
	protected RuntimeException illegalJoin() {
		return new IllegalArgumentException( "UPDATE/DELETE criteria queries cannot define joins" );
	}

	public void renderFrom(RenderingContext rc) {
	    if (getAlias() != null) {
    		rc.append(getModel().getTableName()).append(" as ").append(getAlias());
	    } else {
            rc.append(getModel().getTableName());
	    }
	}

	@Override
	public String getPathAlias() {
		return getAlias();
	}

	X $ = null;
    @Override
    public synchronized X $() {
        if ($ == null) {
            $ = GetterInterceptor.create(getEntityType().getJavaType());
        }
        return $;
    }

    @Override
    public <Y> Path<Y> get(Y attribute) {
        String attributeName = GetterInterceptor.take();
        return getAttr(attributeName);
    }

    public <Y> Path<Y> getAttr(String attributeName) {
        Attribute attr = getModel().locateAttribute(attributeName);

        Path<Y> path = new AttributePathImpl<Y>(criteriaBuilder(), attr.getType(), this, (Attribute<X, Y>) attr);
        return path;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public Root<X> alias(String name) {
        alias = name;
        return this;
    }

    @Override
    public void prepareAlias(RenderingContext rc) {
        if ( getAlias() == null ) {
            alias( rc.generateAlias() );
        }
    }

    @Override
    public String toString() {
        return "[" + entityType.getName() + (getAlias() != null ? " as " + getAlias() : "") + "]";
    }
    
    
}
