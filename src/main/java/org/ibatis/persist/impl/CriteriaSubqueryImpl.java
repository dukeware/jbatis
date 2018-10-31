package org.ibatis.persist.impl;

import java.util.List;
import java.util.Set;

import org.ibatis.persist.criteria.AbstractQuery;
import org.ibatis.persist.criteria.CommonAbstractCriteria;
import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.criteria.Root;
import org.ibatis.persist.criteria.Subquery;
import org.ibatis.persist.impl.expression.DelegatedExpressionImpl;
import org.ibatis.persist.impl.expression.ExpressionImpl;
import org.ibatis.persist.meta.EntityType;

/**
 * The Hibernate implementation of the JPA {@link Subquery} contract.  Mostlty a set of delegation to its internal
 * {@link QueryStructure}.
 */
@SuppressWarnings("unchecked")
public class CriteriaSubqueryImpl<T> extends ExpressionImpl<T> implements Subquery<T> {
	private final CommonAbstractCriteria parent;
	private final QueryStructure<T> queryStructure;

	public CriteriaSubqueryImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			CommonAbstractCriteria parent) {
		super( criteriaBuilder, javaType);
		this.parent = parent;
		this.queryStructure = new QueryStructure<T>( this, criteriaBuilder, javaType);
	}

	@Override
	public AbstractQuery<?> getParent() {
		if ( ! AbstractQuery.class.isInstance( parent ) ) {
			throw new IllegalStateException( "Cannot call getParent on update/delete criterias" );
		}
		return (AbstractQuery<?>) parent;
	}

	@Override
	public CommonAbstractCriteria getContainingQuery() {
		return parent;
	}

	@Override
	public Class<T> getResultType() {
		return getJavaType();
	}


	// ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Set<Root<?>> getRoots() {
		return queryStructure.getRoots();
	}

	@Override
	public <X> Root<X> from(Class<X> entityClass) {
		return queryStructure.from( entityClass );
	}

    @Override
    public <X> Root<X> from(EntityType<X> entityType) {
        return queryStructure.from( entityType );
    }

	// SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Subquery<T> distinct(boolean applyDistinction) {
		queryStructure.setDistinct( applyDistinction );
		return this;
	}

	@Override
	public boolean isDistinct() {
		return queryStructure.isDistinct();
	}

	private Expression<T> wrappedSelection;

	@Override
	public Expression<T> getSelection() {
		if ( wrappedSelection == null ) {
			if ( queryStructure.getSelection() == null ) {
				return null;
			}
			wrappedSelection = new SubquerySelection<T>( (ExpressionImpl<T>) queryStructure.getSelection(), this );
		}
		return wrappedSelection;
	}

	@Override
	public Subquery<T> select(Expression<T> expression) {
		queryStructure.setSelection( expression );
		return this;
	}


	public static class SubquerySelection<S> extends DelegatedExpressionImpl<S> {
		private final CriteriaSubqueryImpl subQuery;

		public SubquerySelection(ExpressionImpl<S> wrapped, CriteriaSubqueryImpl subQuery) {
			super( wrapped );
			this.subQuery = subQuery;
		}

		@Override
        public void render(RenderingContext rc) {
            subQuery.render(rc);
        }

		@Override
        public void renderProjection(RenderingContext rc) {
            render(rc);
        }
	}


	// RESTRICTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Predicate getRestriction() {
		return queryStructure.getRestriction();
	}

	@Override
	public Subquery<T> where(Expression<Boolean> expression) {
		queryStructure.setRestriction( criteriaBuilder().wrap( expression ) );
		return this;
	}

	@Override
	public Subquery<T> where(Predicate... predicates) {
		// TODO : assuming this should be a conjuntion, but the spec does not say specifically...
		queryStructure.setRestriction( criteriaBuilder().and( predicates ) );
		return this;
	}



	// GROUPING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public List<Expression<?>> getGroupList() {
		return queryStructure.getGroupings();
	}

	@Override
	public Subquery<T> groupBy(Expression<?>... groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	@Override
	public Subquery<T> groupBy(List<Expression<?>> groupings) {
		queryStructure.setGroupings( groupings );
		return this;
	}

	@Override
	public Predicate getGroupRestriction() {
		return queryStructure.getHaving();
	}

	@Override
	public Subquery<T> having(Expression<Boolean> expression) {
		queryStructure.setHaving( criteriaBuilder().wrap( expression ) );
		return this;
	}

	@Override
	public Subquery<T> having(Predicate... predicates) {
		queryStructure.setHaving( criteriaBuilder().and( predicates ) );
		return this;
	}


	// CORRELATIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <U> Subquery<U> subquery(Class<U> subqueryType) {
		return queryStructure.subquery( subqueryType );
	}


	// rendering ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
    public void render(RenderingContext rc) {
	    boolean query = rc.isQuery();
        rc.append("(");
        rc.setQuery(true);
        queryStructure.render(rc);
        rc.setQuery(query);
        rc.append(')');
    }

	@Override
	public void renderProjection(RenderingContext rc) {
		throw new IllegalStateException( "Subquery cannot occur in select clause" );
	}
}
