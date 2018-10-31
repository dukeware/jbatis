package org.ibatis.persist.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.ibatis.persist.Parameter;
import org.ibatis.persist.criteria.CommonAbstractCriteria;
import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.criteria.Root;
import org.ibatis.persist.criteria.Subquery;
import org.ibatis.persist.impl.path.RootImpl;
import org.ibatis.persist.meta.EntityType;

import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMapping;

@SuppressWarnings("unchecked")
public abstract class CriteriaManipulation<T> implements CriteriaStatement, CommonAbstractCriteria {
    private final CriteriaBuilderImpl criteriaBuilder;

    private RootImpl<T> root;
    private Predicate restriction;
    // private List<Subquery<?>> subQueries;

    protected CriteriaManipulation(CriteriaBuilderImpl criteriaBuilder, Class<T> entityClass) {
        this.criteriaBuilder = criteriaBuilder;
        EntityType<T> entityType = criteriaBuilder.getEntityManager().initEntityClass(entityClass);
        if (entityType != null && !entityType.isFailed()) {
            from(entityType);
        }
    }

    protected CriteriaBuilderImpl criteriaBuilder() {
        return criteriaBuilder;
    }

    // Root ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Root<T> from(Class<T> entityClass) {
        EntityType<T> entityType = criteriaBuilder.getEntityManager().initEntityClass(entityClass);
        if (entityType == null || entityType.isFailed()) {
            throw new IllegalArgumentException(entityClass + " is not an entity");
        }
        return from(entityType);
    }

    public Root<T> from(EntityType<T> entityType) {
        if (entityType == null || entityType.isFailed()) {
            throw new IllegalArgumentException("null or bad entity");
        }
        root = new RootImpl<T>(criteriaBuilder, entityType, null);
        return root;
    }

    public Root<T> getRoot() {
        return root;
    }

    // Restriction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    protected void setRestriction(Expression<Boolean> restriction) {
        this.restriction = criteriaBuilder.wrap(restriction);
    }

    public void setRestriction(Predicate... restrictions) {
        this.restriction = criteriaBuilder.and(restrictions);
    }

    public Predicate getRestriction() {
        return restriction;
    }

    public <U> Subquery<U> subquery(Class<U> type) {
        return new CriteriaSubqueryImpl<U>(criteriaBuilder(), type, this);
    }

    // compiling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    protected void validate() {
        if (root == null) {
            throw new IllegalStateException("UPDATE/DELETE criteria must name root entity");
        }
    }

    protected abstract void renderQuery(RenderingContext rc);

    protected void renderRoot(RenderingContext rc) {
        ((FromImplementor) root).renderFrom(rc);
    }

    protected void renderRestrictions(RenderingContext rc) {
        if (getRestriction() != null) {
            rc.append(" where ");
            ((Renderable) getRestriction()).render(rc);
        }
    }

    RenderingContext rc;
    @Override
    public synchronized void prepare() {
        if (rc == null) {
            validate();
            rc = new RenderingContext();
            rc.setQuery(false);
            renderQuery(rc);
        }
    }

    @Override
    public boolean isQuery() {
        return false;
    }

    @Override
    public String getSql() {
        prepare();
        return rc.getSql();
    }

    @Override
    public ParameterInfo<?>[] getParameterInfos() {
        prepare();
        return rc.getParameterInfos();
    }

    public Set<? extends Parameter<?>> getParameters() {
        prepare();
        return rc.getParameters();
    }

    public Parameter<?> getParameter(String name) {
        for (Parameter<?> p : getParameters()) {
            if (name.equals(p.getName())) {
                return p;
            }
        }
        throw new IllegalArgumentException(name);
    }

    public <R> Parameter<R> getParameter(String name, Class<R> type) {
        for (Parameter<?> p : getParameters()) {
            if (name.equals(p.getName()) && type.isAssignableFrom(p.getParameterType())) {
                return (Parameter<R>) p;
            }
        }
        throw new IllegalArgumentException(name);
    }

    public boolean isBound(Parameter<?> param) {
        Object r = ((ParameterInfo<?>) param).getParameterValue();
        return r != ParameterInfo.None;
    }

    public <R> R getParameterValue(Parameter<R> param) {
        R r = ((ParameterInfo<R>) param).getParameterValue();
        if (r == ParameterInfo.None) {
            return null;
        }
        return r;
    }

    public <R> R getParameterValue(String name) {
        return getParameterValue((Parameter<R>) getParameter(name));
    }
    
    ParameterMap parameterMap;
    @Override
    public ParameterMap makeParameterMap(SqlMapExecutorDelegate delegate) {
        if (parameterMap == null) {
            parameterMap = new ParameterMap(delegate);
            List<ParameterMapping> maps = new ArrayList<ParameterMapping>();
            for (ParameterInfo<?> pi : getParameterInfos()) {
                ParameterMapping map = new ParameterMapping();
                map.setMode("IN");
                map.setTypeHandler(delegate.getTypeHandlerFactory().getTypeHandler(pi.getParameterType()));
                map.setJavaType(pi.getParameterType());
                maps.add(map);
            }
            parameterMap.setParameterMappingList(maps);
        }
        return parameterMap;
    }

    @Override
    public void flushCache(SqlMapExecutorDelegate delegate) {
        if (root != null && root.getEntityType().isCacheable()) {
            delegate.getEntityManager().flushEntityCache(root.getEntityType().getJavaType());
        }
    }

    @Override
    public EntityType<?> getQueryCacheType() {
        return null;
    }
}
