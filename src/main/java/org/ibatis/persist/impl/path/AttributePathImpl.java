package org.ibatis.persist.impl.path;

import org.ibatis.persist.meta.Attribute;
import org.ibatis.persist.criteria.Path;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.PathImplementor;
import org.ibatis.persist.impl.PathSource;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.expression.ExpressionImpl;

public class AttributePathImpl<X> extends ExpressionImpl<X>implements Path<X>, PathImplementor<X> {

    private final PathSource pathSource;

    private final Attribute<?, X> attribute;

    public AttributePathImpl(CriteriaBuilderImpl criteriaBuilder, Class<X> javaType, PathSource pathSource,
        Attribute<?, X> attribute) {
        super(criteriaBuilder, javaType);
        this.pathSource = pathSource;
        this.attribute = attribute;
    }

    public PathSource getPathSource() {
        return pathSource;
    }

    @Override
    public void render(RenderingContext rc) {
        PathSource source = getPathSource();
        Attribute<?, ?> a = getAttribute();
        if (source != null && source.getPathAlias() != null) {
            rc.append(source.getPathAlias() + "." + a.getColumn());
        } else {
            rc.append(a.getColumn());
        }
    }

    @Override
    public void renderProjection(RenderingContext rc) {
        render(rc);
        if (getAlias() == null) {
            alias(getAttribute().getName());
        }
        rc.append(" AS ").append(getAlias());
    }

    @Override
    public Attribute<?, X> getAttribute() {
        return attribute;
    }

}
