package org.ibatis.persist.impl;

import org.ibatis.persist.Parameter;
import org.ibatis.persist.criteria.CriteriaDelete;
import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Predicate;

@SuppressWarnings("unchecked")
public class CriteriaDeleteImpl<T> extends CriteriaManipulation<T>implements CriteriaDelete<T> {
    
    protected CriteriaDeleteImpl(CriteriaBuilderImpl criteriaBuilder, Class<T> targetEntity) {
        super(criteriaBuilder, targetEntity);
    }

    @Override
    public CriteriaDelete<T> where(Expression<Boolean> restriction) {
        setRestriction(restriction);
        return this;
    }

    @Override
    public CriteriaDelete<T> where(Predicate... restrictions) {
        setRestriction(restrictions);
        return this;
    }

    @Override
    protected void renderQuery(RenderingContext rc) {
        rc.append("delete from ");
        renderRoot(rc);
        renderRestrictions(rc);
    }

    @Override
    public <R> CriteriaDelete<T> setParameter(Parameter<R> param, R value) {
        ParameterInfo<R> pi = (ParameterInfo<R>) param;
        pi.setParameterValue(value);
        return this;
    }

    @Override
    public <R> CriteriaDelete<T> setParameter(String name, R value) {
        return setParameter((Parameter<R>) getParameter(name), value);
    }

    @Override
    public <R> CriteriaDelete<T> setParameter(int position, R value) {
        for (Parameter<?> p : getParameters()) {
            if (p.getPosition() != null && p.getPosition() == position) {
                setParameter((Parameter<R>) p, value);
                return this;
            }
        }
        throw new IllegalArgumentException("" + position);
    }

    @Override
    public Class<?> getResultType() {
        return Integer.class;
    }

}
