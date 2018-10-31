package org.ibatis.persist.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.ibatis.persist.Entity;
import org.ibatis.persist.criteria.AbstractQuery;
import org.ibatis.persist.criteria.Expression;
import org.ibatis.persist.criteria.Predicate;
import org.ibatis.persist.criteria.Root;
import org.ibatis.persist.criteria.Selection;
import org.ibatis.persist.criteria.Subquery;
import org.ibatis.persist.impl.path.RootImpl;
import org.ibatis.persist.meta.EntityType;
/**
 * QueryStructure
 */
@SuppressWarnings("unchecked")
public class QueryStructure<T> {
	private final AbstractQuery<T> owner;
	private final CriteriaBuilderImpl criteriaBuilder;
	private final boolean isSubQuery;
    private Class<T> resultType;

	public QueryStructure(AbstractQuery<T> owner, CriteriaBuilderImpl criteriaBuilder, Class<T> resultType) {
		this.owner = owner;
		this.criteriaBuilder = criteriaBuilder;
		this.resultType = resultType;
		this.isSubQuery = Subquery.class.isInstance( owner );
	}

	private boolean distinct;
	private Selection<? extends T> selection;
	private Set<Root<?>> roots = new LinkedHashSet<Root<?>>();
	private JoinImplementor joinImplementor;
	private Predicate restriction;
	private List<Expression<?>> groupings = Collections.emptyList();
	private Predicate having;
	private List<Subquery<?>> subqueries;


	// SELECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public Selection<? extends T> getSelection() {
		return selection;
	}

	public void setSelection(Selection<? extends T> selection) {
		this.selection = selection;
	}


	// ROOTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Set<Root<?>> getRoots() {
		return roots;
	}

	public <X> Root<X> from(Class<X> entityClass) {
		EntityType<X> entityType = criteriaBuilder.getEntityManager()
				.initEntityClass( entityClass );
		if (entityType == null || entityType.isFailed()) {
			throw new IllegalArgumentException( entityClass + " is not an entity" );
		}
		return from( entityType );
	}

	public <X> Root<X> from(EntityType<X> entityType) {
		RootImpl<X> root = new RootImpl<X>( criteriaBuilder, entityType, this);
		roots.add( root );
		return root;
	}

	// RESTRICTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Predicate getRestriction() {
		return restriction;
	}

	public void setRestriction(Predicate restriction) {
		this.restriction = restriction;
	}


	// GROUPINGS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Expression<?>> getGroupings() {
		return groupings;
	}

	public void setGroupings(List<Expression<?>> groupings) {
		this.groupings = groupings;
	}

	public void setGroupings(Expression<?>... groupings) {
		if ( groupings != null && groupings.length > 0 ) {
			this.groupings = Arrays.asList( groupings );
		}
		else {
			this.groupings = Collections.emptyList();
		}
	}

	public Predicate getHaving() {
		return having;
	}

	public void setHaving(Predicate having) {
		this.having = having;
	}


	// SUB-QUERIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public List<Subquery<?>> getSubqueries() {
		return subqueries;
	}

	public List<Subquery<?>> internalGetSubqueries() {
		if ( subqueries == null ) {
			subqueries = new ArrayList<Subquery<?>>();
		}
		return subqueries;
	}

	public <U> Subquery<U> subquery(Class<U> subqueryType) {
		CriteriaSubqueryImpl<U> subquery = new CriteriaSubqueryImpl<U>( criteriaBuilder, subqueryType, owner );
		internalGetSubqueries().add( subquery );
		return subquery;
	}

    public void render(RenderingContext rc) {
        Set<Root> joinedRoots = new HashSet<Root>();
        if (joinImplementor != null) {
            collectJoinedRoots(joinImplementor, joinedRoots);
        }
        //Set<Root> allRoots = new HashSet<Root>();
        //allRoots.addAll(getRoots());
        //allRoots.addAll(joinedRoots);
        Set<Root<?>> allRoots = getRoots();
        if (allRoots.size() > 1) {
            for (Root<?> root : allRoots) {
                ((RootImpl) root).prepareAlias(rc);
            }
        }
        rc.append("select ");
        if (isDistinct()) {
            rc.append("distinct ");
        }
        if (getSelection() == null) {
            locateImplicitSelection().renderProjection(rc);
        } else {
            ((Renderable) getSelection()).renderProjection(rc);
        }

        renderFromClause(rc, joinedRoots);

        if (getRestriction() != null) {
            rc.append(" where ");
            ((Renderable) getRestriction()).render(rc);
        }

        if (!getGroupings().isEmpty()) {
            rc.append(" group by ");
            String sep = "";
            for (Expression grouping : getGroupings()) {
                rc.append(sep);
                ((Renderable) grouping).render(rc);
                sep = ", ";
            }

            if (getHaving() != null) {
                rc.append(" having ");
                ((Renderable) getHaving()).render(rc);
            }
        }
    }

    private Renderable locateImplicitSelection() {
        FromImplementor implicitSelection = null;

        if (!isSubQuery) {
            // we should have only a single root (query validation should have checked this...)
            implicitSelection = (FromImplementor) getRoots().iterator().next();
        } else {
            if (resultType.getAnnotation(Entity.class) != null) {
                implicitSelection = (FromImplementor) from(resultType);
            }
        }

        if (implicitSelection instanceof RootImpl) {
            RootImpl root = (RootImpl) implicitSelection;
            List<String> attrs = root.getEntityType().getAttributeNames();
            List<Selection<?>> list = new ArrayList<Selection<?>>();
            
            for (String attr:attrs) {
                list.add(root.getAttr(attr));
            }
            return (Renderable) criteriaBuilder.construct(resultType, list);
        }
        throw new IllegalStateException("No explicit selection and an implicit one could not be determined");
        
    }

    private void renderFromClause(RenderingContext rc, Set<Root> joinedRoots) {
        rc.append(" from ");
        String sep = "";
        for (Root root : getRoots()) {
            if (joinedRoots.contains(root)) {
                continue;
            }
            rc.append(sep);
            ((RootImpl) root).renderFrom(rc);
            sep = ", ";
        }

        if (joinImplementor != null) {
            rc.append(sep);
            joinImplementor.renderFrom(rc);
        }

    }

    void collectJoinedRoots(JoinImplementor join, Set<Root> joinedRoots) {
        if (join.getLeft() instanceof Root) {
            joinedRoots.add((Root) join.getLeft());
        } else if (join.getLeft() instanceof JoinImplementor) {
            collectJoinedRoots((JoinImplementor) join.getLeft(), joinedRoots);
        }
        if (join.getRight() instanceof Root) {
            joinedRoots.add((Root) join.getRight());
        } else if (join.getRight() instanceof JoinImplementor) {
            collectJoinedRoots((JoinImplementor) join.getLeft(), joinedRoots);
        }
    }

    public void join(JoinImplementor join) {
        if (joinImplementor != null && joinImplementor != join.getLeft()) {
            throw new IllegalArgumentException("bad join order");
        }
        joinImplementor = join;
    }

}
