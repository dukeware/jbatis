package org.ibatis.persist.impl;


public abstract class AbstractNode {
    private final CriteriaBuilderImpl criteriaBuilder;

    public AbstractNode(CriteriaBuilderImpl criteriaBuilder) {
        this.criteriaBuilder = criteriaBuilder;
    }

    public CriteriaBuilderImpl criteriaBuilder() {
        return criteriaBuilder;
    }
}
