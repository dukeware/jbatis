package org.ibatis.persist.impl;

import java.util.ArrayList;
import java.util.List;

import org.ibatis.persist.Parameter;
import org.ibatis.persist.criteria.CriteriaUpdate;
import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Path;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.impl.path.AttributePathImpl;
import org.ibatis.persist.impl.util.GetterInterceptor;

@SuppressWarnings("unchecked")
public class CriteriaUpdateImpl<T> extends CriteriaManipulation<T> implements CriteriaUpdate<T> {
    private List<Assignment> assignments = new ArrayList<Assignment>();

    public CriteriaUpdateImpl(CriteriaBuilderImpl criteriaBuilder, Class<T> targetEntity) {
        super(criteriaBuilder, targetEntity);
    }


    @Override
    public <Y, X extends Y> CriteriaUpdate<T> set(Path<Y> attributePath, X value) {
        final Expression valueExpression = value == null ? criteriaBuilder().nullLiteral(attributePath.getJavaType())
                : criteriaBuilder().literal(value);
        addAssignment(attributePath, valueExpression);
        return this;
    }

    @Override
    public <Y> CriteriaUpdate<T> set(Path<Y> attributePath, Expression<? extends Y> value) {
        addAssignment(attributePath, value);
        return this;
    }

    @Override
    public <Y> CriteriaUpdate<T> set(Y attribute, Y value) {
        String attributeName = GetterInterceptor.take();
        
        final Path attributePath = getRoot().get(attributeName);
        final Expression valueExpression = value == null ? criteriaBuilder().nullLiteral(attributePath.getJavaType())
                : criteriaBuilder().literal(value);
        addAssignment(attributePath, valueExpression);
        return this;
    }


    @Override
    public <Y> CriteriaUpdate<T> set(Y attribute, Expression<? extends Y> value) {
        String attributeName = GetterInterceptor.take();
        
        final Path attributePath = getRoot().get(attributeName);
        addAssignment(attributePath, value);
        return this;
    }
    
    protected <Y> void addAssignment(Path<Y> attributePath, Expression<? extends Y> value) {
        if (value == null) {
            throw new IllegalArgumentException(
                "Assignment value expression cannot be null. Did you mean to pass null as a literal?");
        }
        assignments.add(new Assignment<Y>((AttributePathImpl<Y>) attributePath, value));
    }

    @Override
    public CriteriaUpdate<T> where(Expression<Boolean> restriction) {
        setRestriction(restriction);
        return this;
    }

    @Override
    public CriteriaUpdate<T> where(Predicate... restrictions) {
        setRestriction(restrictions);
        return this;
    }

    @Override
    public void validate() {
        super.validate();
        if (assignments.isEmpty()) {
            throw new IllegalStateException("No assignments specified as part of UPDATE criteria");
        }
    }

    @Override
    protected void renderQuery(RenderingContext rc) {
        rc.append("update ");
        renderRoot( rc);
        renderAssignments( rc);
        renderRestrictions( rc);
    }

    private void renderAssignments(RenderingContext rc) {
        rc.append(" set ");
        boolean first = true;
        for (Assignment assignment : assignments) {
            if (!first) {
                rc.append(", ");
            }
            assignment.attributePath.render(rc);
            rc.append(" = ");
            assignment.value.render(rc);
            first = false;
        }
    }

    private class Assignment<A> {
        private final AttributePathImpl<A> attributePath;
        private final ExpressionImplementor<? extends A> value;

        private Assignment(AttributePathImpl<A> attributePath, Expression<? extends A> value) {
            this.attributePath = attributePath;
            this.value = (ExpressionImplementor) value;
        }
    }

    @Override
    public <R> CriteriaUpdate<T> setParameter(Parameter<R> param, R value) {
        ParameterInfo<R> pi = (ParameterInfo<R>) param;
        pi.setParameterValue(value);
        return this;
    }


    @Override
    public <R> CriteriaUpdate<T> setParameter(String name, R value) {
        return setParameter((Parameter<R>) getParameter(name), value);
    }


    @Override
    public <R> CriteriaUpdate<T> setParameter(int position, R value) {
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
