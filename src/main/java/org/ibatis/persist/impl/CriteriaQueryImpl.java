package org.ibatis.persist.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.ibatis.persist.Parameter;
import org.ibatis.persist.criteria.CriteriaQuery;
import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Order;
import org.ibatis.persist.criteria.ParameterExpression;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.criteria.Root;
import org.ibatis.persist.criteria.Selection;
import org.ibatis.persist.criteria.Subquery;
import org.ibatis.persist.meta.EntityType;

import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMap;
import com.ibatis.sqlmap.engine.mapping.parameter.ParameterMapping;

/**
 * The Hibernate implementation of the JPA {@link CriteriaQuery} contract. Mostly a set of delegation to its internal
 * {@link QueryStructure}.
 */
@SuppressWarnings("unchecked")
public class CriteriaQueryImpl<T> extends AbstractNode implements CriteriaQuery<T>, CriteriaStatement {

    private final Class<T> returnType;

    private final QueryStructure<T> queryStructure;
    private List<Order> orderSpecs = Collections.emptyList();

    public CriteriaQueryImpl(CriteriaBuilderImpl criteriaBuilder, Class<T> returnType) {
        super(criteriaBuilder);
        this.returnType = returnType;
        this.queryStructure = new QueryStructure<T>(this, criteriaBuilder, returnType);
    }

    @Override
    public Class<T> getResultType() {
        return returnType;
    }

    // SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public CriteriaQuery<T> distinct(boolean applyDistinction) {
        queryStructure.setDistinct(applyDistinction);
        return this;
    }

    @Override
    public boolean isDistinct() {
        return queryStructure.isDistinct();
    }

    @Override
    public Selection<T> getSelection() {
        return (Selection<T>) queryStructure.getSelection();
    }

    public void applySelection(Selection<? extends T> selection) {
        queryStructure.setSelection(selection);
    }

    @Override
    public CriteriaQuery<T> select(Selection<? extends T> selection) {
        applySelection(selection);
        return this;
    }

    @Override
    public CriteriaQuery<T> multiselect(Selection<?>... selections) {
        return multiselect(Arrays.asList(selections));
    }

    @Override
    public CriteriaQuery<T> multiselect(List<Selection<?>> selections) {
        final Selection<? extends T> selection;

        selection = criteriaBuilder().construct(getResultType(), selections);
        applySelection(selection);
        return this;
    }

    // ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public Set<Root<?>> getRoots() {
        return queryStructure.getRoots();
    }

    @Override
    public <X> Root<X> from(Class<X> entityClass) {
        return queryStructure.from(entityClass);
    }

    @Override
    public <X> Root<X> from(EntityType<X> entityType) {
        return queryStructure.from(entityType);
    }
    
    // RESTRICTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public Predicate getRestriction() {
        return queryStructure.getRestriction();
    }

    @Override
    public CriteriaQuery<T> where(Expression<Boolean> expression) {
        queryStructure.setRestriction(criteriaBuilder().wrap(expression));
        return this;
    }

    @Override
    public CriteriaQuery<T> where(Predicate... predicates) {
        // TODO : assuming this should be a conjuntion, but the spec does not say specifically...
        queryStructure.setRestriction(criteriaBuilder().and(predicates));
        return this;
    }

    // GROUPING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public List<Expression<?>> getGroupList() {
        return queryStructure.getGroupings();
    }

    @Override
    public CriteriaQuery<T> groupBy(Expression<?>... groupings) {
        queryStructure.setGroupings(groupings);
        return this;
    }

    @Override
    public CriteriaQuery<T> groupBy(List<Expression<?>> groupings) {
        queryStructure.setGroupings(groupings);
        return this;
    }

    @Override
    public Predicate getGroupRestriction() {
        return queryStructure.getHaving();
    }

    @Override
    public CriteriaQuery<T> having(Expression<Boolean> expression) {
        queryStructure.setHaving(criteriaBuilder().wrap(expression));
        return this;
    }

    @Override
    public CriteriaQuery<T> having(Predicate... predicates) {
        queryStructure.setHaving(criteriaBuilder().and(predicates));
        return this;
    }

    // ORDERING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    public List<Order> getOrderList() {
        return orderSpecs;
    }

    @Override
    public CriteriaQuery<T> orderBy(Order... orders) {
        if (orders != null && orders.length > 0) {
            orderSpecs = Arrays.asList(orders);
        } else {
            orderSpecs = Collections.emptyList();
        }
        return this;
    }

    @Override
    public CriteriaQuery<T> orderBy(List<Order> orders) {
        orderSpecs = orders;
        return this;
    }

    @Override
    public Set<ParameterExpression<?>> getParameters() {
        prepare();
        return rc.getParameters();
    }

    @Override
    public <U> Subquery<U> subquery(Class<U> subqueryType) {
        return queryStructure.subquery(subqueryType);
    }

    void validate() {
        // getRoots() is explicitly supposed to return empty if none defined, no need to check for null
        if (getRoots().isEmpty()) {
            throw new IllegalStateException("No criteria query roots were specified");
        }

        // if there is not an explicit selection, there is an *implicit* selection of the root entity provided only
        // a single query root was defined.
        if (getSelection() == null && !hasImplicitSelection()) {
            throw new IllegalStateException("No explicit selection and an implicit one could not be determined");
        }
    }

    /**
     * If no explicit selection was defined, we have a condition called an implicit selection if the query specified a
     * single {@link Root} and the java type of that {@link Root root's} model is the same as this criteria's
     * {@link #getResultType() result type}.
     *
     * @return True if there is an explicit selection; false otherwise.
     */
    private boolean hasImplicitSelection() {
        if (getRoots().size() != 1) {
            return false;
        }

        Root root = getRoots().iterator().next();
        Class<?> javaType = root.getModel().getJavaType();
        if (javaType != null && javaType != returnType) {
            return false;
        }

        return true;
    }


    @Override
    public <R> CriteriaQuery<T> setParameter(Parameter<R> param, R value) {
        ParameterInfo<R> pi = (ParameterInfo<R>) param;
        pi.setParameterValue(value);
        return this;
    }

    @Override
    public <R> CriteriaQuery<T> setParameter(String name, R value) {
        return setParameter((Parameter<R>) getParameter(name), value);
    }

    @Override
    public <R> CriteriaQuery<T> setParameter(int position, R value) {
        for (Parameter<?> p : getParameters()) {
            if (p.getPosition() != null && p.getPosition() == position) {
                setParameter((Parameter<R>) p, value);
                return this;
            }
        }
        throw new IllegalArgumentException("" + position);
    }

    @Override
    public Parameter<?> getParameter(String name) {
        for (Parameter<?> p : getParameters()) {
            if (name.equals(p.getName())) {
                return p;
            }
        }
        throw new IllegalArgumentException(name);
    }

    @Override
    public <R> Parameter<R> getParameter(String name, Class<R> type) {
        for (Parameter<?> p : getParameters()) {
            if (name.equals(p.getName()) && type.isAssignableFrom(p.getParameterType())) {
                return (Parameter<R>) p;
            }
        }
        throw new IllegalArgumentException(name);
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        Object r = ((ParameterInfo<?>) param).getParameterValue();
        return r != ParameterInfo.None;
    }

    @Override
    public <R> R getParameterValue(Parameter<R> param) {
        R r = ((ParameterInfo<R>) param).getParameterValue();
        if (r == ParameterInfo.None) {
            return null;
        }
        return r;
    }

    @Override
    public <R> R getParameterValue(String name) {
        return getParameterValue((Parameter<R>) getParameter(name));
    }

    RenderingContext rc;
    @Override
    public synchronized void prepare() {
        if (rc == null) {
            rc = new RenderingContext();
            validate();
            queryStructure.render(rc);

            if (!getOrderList().isEmpty()) {
                rc.append(" order by ");
                String sep = "";
                for (Order orderSpec : getOrderList()) {
                    rc.append(sep);
                    ((Renderable) orderSpec.getExpression()).render(rc);
                    rc.append(orderSpec.isAscending() ? " asc" : " desc");
                    sep = ", ";
                }
            }
        }
    }

    @Override
    public boolean isQuery() {
        return true;
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
        // ## do nothing
    }

    @Override
    public EntityType<?> getQueryCacheType() {
        Set<Root<?>> roots = queryStructure.getRoots();
        if (roots.size() == 1) {
            Root<?> root = roots.iterator().next();
            if (root.getModel().isCacheable()) {
                return root.getModel();
            }
        }
        return null;
    }

}
