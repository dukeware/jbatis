package org.ibatis.persist.impl.expression;

import java.util.List;

import org.ibatis.persist.criteria.Selection;

import com.ibatis.sqlmap.engine.type.TypeHandler;

/**
 * Implementation of {@link org.ibatis.persist.criteria.Expression} wraps another Expression and delegates most of its
 * functionality to that wrapped Expression
 */
public abstract class DelegatedExpressionImpl<T> extends ExpressionImpl<T> {
	private final ExpressionImpl<T> wrapped;

	public DelegatedExpressionImpl(ExpressionImpl<T> wrapped) {
		super( wrapped.criteriaBuilder(), wrapped.getJavaType() );
		this.wrapped = wrapped;
	}

	public ExpressionImpl<T> getWrapped() {
		return wrapped;
	}


	// delegations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Selection<T> alias(String alias) {
		wrapped.alias( alias );
		return this;
	}

	@Override
	public boolean isCompoundSelection() {
		return wrapped.isCompoundSelection();
	}

	@Override
	public List<TypeHandler<?>> getValueHandlers() {
		return wrapped.getValueHandlers();
	}

	@Override
	public List<Selection<?>> getCompoundSelectionItems() {
		return wrapped.getCompoundSelectionItems();
	}

	@Override
	public Class<T> getJavaType() {
		return wrapped.getJavaType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setJavaType(Class targetType) {
		wrapped.setJavaType( targetType );
	}

	@Override
	protected void forceConversion(TypeHandler<T> tValueHandler) {
		wrapped.forceConversion( tValueHandler );
	}

	@Override
	public TypeHandler<T> getValueHandler() {
		return wrapped.getValueHandler();
	}

	@Override
	public String getAlias() {
		return wrapped.getAlias();
	}

	@Override
	protected void setAlias(String alias) {
		wrapped.setAlias( alias );
	}
}
