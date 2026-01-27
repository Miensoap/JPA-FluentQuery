package me.miensoap.fluent.core;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Sort;

public final class OrderStep<T> {

    private final FluentQuery<T> query;
    private final String field;

    OrderStep(@NotNull FluentQuery<T> query, @NotNull String field) {
        this.query = query;
        this.field = field;
    }

    @NotNull
    public FluentQuery<T> ascending() {
        query.addOrder(Sort.Order.asc(field));
        return query;
    }

    @NotNull
    public FluentQuery<T> descending() {
        query.addOrder(Sort.Order.desc(field));
        return query;
    }
}
