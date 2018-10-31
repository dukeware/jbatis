package org.ibatis.persist.impl.expression;

import java.util.ArrayList;
import java.util.List;

import org.ibatis.persist.criteria.CompoundSelection;
import org.ibatis.persist.criteria.Selection;
import org.ibatis.persist.impl.CriteriaBuilderImpl;
import org.ibatis.persist.impl.Renderable;
import org.ibatis.persist.impl.RenderingContext;
import org.ibatis.persist.impl.SelectionImplementor;

import com.ibatis.sqlmap.engine.type.TypeHandler;

/**
 * The Hibernate implementation of the JPA {@link CompoundSelection}
 * contract.
 */
@SuppressWarnings("unchecked")
public class CompoundSelectionImpl<X>
		extends SelectionImpl<X>
		implements CompoundSelection<X> {
	private List<Selection<?>> selectionItems;

	public CompoundSelectionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			List<Selection<?>> selectionItems) {
		super( criteriaBuilder, javaType );
		this.selectionItems = selectionItems;
	}

	@Override
	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public List<Selection<?>> getCompoundSelectionItems() {
		return selectionItems;
	}

	@Override
    public List<TypeHandler<?>> getValueHandlers() {
        boolean foundHandlers = false;
        ArrayList<TypeHandler<?>> valueHandlers = new ArrayList<TypeHandler<?>>();
        for (Selection selection : getCompoundSelectionItems()) {
            TypeHandler<?> valueHandler = ((SelectionImplementor) selection).getValueHandler();
            valueHandlers.add(valueHandler);
            foundHandlers = foundHandlers || valueHandler != null;
        }
        return foundHandlers ? null : valueHandlers;
    }

    public void render(RenderingContext rc) {
        throw new IllegalStateException( "Compound selection cannot occur in expressions" );
    }

    public void renderProjection(RenderingContext rc) {
        String sep = "";
        for (Selection selection : selectionItems) {
            rc.append(sep);
            ((Renderable) selection).renderProjection(rc);
            sep = ", ";
        }
    }
}
