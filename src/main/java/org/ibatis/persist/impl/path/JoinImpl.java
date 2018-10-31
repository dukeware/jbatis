package org.ibatis.persist.impl.path;

import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.JoinType;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.FromImplementor;
import org.ibatis.persist.impl.JoinImplementor;
import org.ibatis.persist.impl.QueryStructure;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.predicate.AbstractPredicateImpl;

@SuppressWarnings("unchecked")
public class JoinImpl extends AbstractFromImpl implements JoinImplementor {

    private final JoinType joinType;

    private FromImplementor left;
    private FromImplementor right;

    private Predicate suppliedJoinCondition;

    public JoinImpl(CriteriaBuilderImpl criteriaBuilder, QueryStructure queryStructure, FromImplementor left,
        FromImplementor right, JoinType jt) {
        super(criteriaBuilder, queryStructure);
        this.left = left;
        this.right = right;
        this.joinType = jt;
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    private String renderJoinType(JoinType joinType) {
        switch (joinType) {
        case LEFT: {
            return " left join ";
        }
        case RIGHT: {
            return " right join ";
        }
        default: {
            return " join ";
        }
        }
    }

    @Override
    public void renderFrom(RenderingContext rc) {
        getLeft().renderFrom(rc);
        rc.append(renderJoinType(joinType));
        getRight().renderFrom(rc);
        if (suppliedJoinCondition != null) {
            rc.append(" on ");
            ((AbstractPredicateImpl) suppliedJoinCondition).render(rc);
        }
    }

    @Override
    public JoinImplementor on(Predicate... restrictions) {
        // no matter what, a call to this method replaces any previously set values...
        this.suppliedJoinCondition = null;

        if (restrictions != null && restrictions.length > 0) {
            this.suppliedJoinCondition = criteriaBuilder().and(restrictions);
        }

        return this;
    }

    @Override
    public JoinImplementor on(Expression<Boolean> restriction) {
        this.suppliedJoinCondition = criteriaBuilder().wrap(restriction);
        return this;
    }

    @Override
    public Predicate getOn() {
        return suppliedJoinCondition;
    }

    @Override
    public FromImplementor getLeft() {
        return left;
    }

    @Override
    public FromImplementor getRight() {
        return right;
    }

    protected RuntimeException illegalJoin() {
        return new IllegalArgumentException("illegal join");
    }

}
