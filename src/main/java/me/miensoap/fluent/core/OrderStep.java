package me.miensoap.fluent.core;

import org.springframework.data.domain.Sort;

public final class OrderStep<T> {

    private final FluentQuery<T> query;
    private final String field;

    OrderStep(FluentQuery<T> query, String field) {
        this.query = query;
        this.field = field;
    }

    public FluentQuery<T> ascending() {
        query.addOrder(Sort.Order.asc(field));
        return query;
    }

    public FluentQuery<T> descending() {
        query.addOrder(Sort.Order.desc(field));
        return query;
    }
}
