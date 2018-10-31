package org.ibatis.persist.impl.path;

import org.ibatis.persist.criteria.From;
import org.ibatis.persist.criteria.Join;
import org.ibatis.persist.criteria.JoinType;
import org.ibatis.persist.impl.AbstractNode;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.FromImplementor;
import org.ibatis.persist.impl.JoinImplementor;
import org.ibatis.persist.impl.QueryStructure;
import org.ibatis.persist.impl.RenderingContext;

@SuppressWarnings("unchecked")
public abstract class AbstractFromImpl extends AbstractNode implements FromImplementor {

    public static final JoinType DEFAULT_JOIN_TYPE = JoinType.INNER;

    QueryStructure queryStructure;

    public AbstractFromImpl(CriteriaBuilderImpl criteriaBuilder, QueryStructure queryStructure) {
        super(criteriaBuilder);
        this.queryStructure = queryStructure;
    }

    protected boolean canBeDereferenced() {
        return true;
    }

    public From getParent() {
        return null;
    }

    // JOINS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    boolean canBeJoinSource() {
        return queryStructure != null;
    }

    private JoinImplementor constructJoin(FromImplementor from, JoinType jt) {
        if (!canBeJoinSource()) {
            throw illegalJoin();
        }
        return new JoinImpl(criteriaBuilder(), queryStructure, this, from, jt);
    }

    protected abstract RuntimeException illegalJoin();

    @Override
    public final Join join(From from) {
        return join(from, DEFAULT_JOIN_TYPE);
    }

    @Override
    public final Join join(From from, JoinType jt) {
        JoinImplementor join = constructJoin((FromImplementor) from, jt);
        queryStructure.join(join);
        return join;
    }

    @Override
    public final void render(RenderingContext rc) {
        throw new IllegalStateException( "Root or join cannot occur in where clause" );
    }

    @Override
    public final void renderProjection(RenderingContext rc) {
        throw new IllegalStateException( "Root or join cannot occur in select clause" );
    }

}
