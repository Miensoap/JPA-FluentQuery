package me.miensoap.fluent.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;

/**
 * Builder that composes Specifications through a fluent API.
 */
public class FluentQuery<T> {

    private final JpaSpecificationExecutor<T> executor;
    private final List<FetchJoinDescriptor> fetchJoins = new ArrayList<>();
    private final List<Sort.Order> orderings = new ArrayList<>();
    private Specification<T> spec;
    private boolean distinct;

    public FluentQuery(@NotNull JpaSpecificationExecutor<T> executor) {
        this.executor = executor;
    }

    @NotNull
    public FieldStep<T> where(@NotNull String field) {
        return new FieldStep<>(this, field, false);
    }

    @NotNull
    public <R> FieldStep<T> where(@NotNull Property<T, R> property) {
        String field = PropertyNameResolver.resolve(property);
        return new FieldStep<>(this, field, false, PropertyNameResolver.resolveType(property));
    }

    @NotNull
    public FluentQuery<T> where(@Nullable Specification<T> specification) {
        this.spec = specification == null ? null : Specification.where(specification);
        return this;
    }

    @NotNull
    public FieldStep<T> and(@NotNull String field) {
        return new FieldStep<>(this, field, false);
    }

    @NotNull
    public <R> FieldStep<T> and(@NotNull Property<T, R> property) {
        String field = PropertyNameResolver.resolve(property);
        return new FieldStep<>(this, field, false, PropertyNameResolver.resolveType(property));
    }

    @NotNull
    public FluentQuery<T> and(@Nullable Specification<T> specification) {
        addCondition(specification, false);
        return this;
    }

    @NotNull
    public FieldStep<T> or(@NotNull String field) {
        return new FieldStep<>(this, field, true);
    }

    @NotNull
    public <R> FieldStep<T> or(@NotNull Property<T, R> property) {
        String field = PropertyNameResolver.resolve(property);
        return new FieldStep<>(this, field, true, PropertyNameResolver.resolveType(property));
    }

    @NotNull
    public FluentQuery<T> or(@Nullable Specification<T> specification) {
        addCondition(specification, true);
        return this;
    }

    @NotNull
    public FluentQuery<T> not() {
        if (spec != null) {
            spec = Specification.not(spec);
        }
        return this;
    }

    @NotNull
    public FluentQuery<T> distinct() {
        this.distinct = true;
        return this;
    }

    @NotNull
    public FluentQuery<T> fetchJoin(@NotNull String path) {
        return fetchJoin(path, JoinType.LEFT);
    }

    @NotNull
    public FluentQuery<T> fetchJoin(@NotNull String path, @NotNull JoinType joinType) {
        return registerFetchJoin(path, joinType);
    }

    @NotNull
    public <R> FluentQuery<T> fetchJoin(@NotNull Property<T, R> property) {
        return fetchJoin(PropertyNameResolver.resolve(property));
    }

    @NotNull
    public <R> FluentQuery<T> fetchJoin(@NotNull Property<T, R> property, @NotNull JoinType joinType) {
        String path = PropertyNameResolver.resolve(property);
        return registerFetchJoin(path, joinType);
    }

    @NotNull
    public OrderStep<T> orderBy(@NotNull String field) {
        return new OrderStep<>(this, field);
    }

    @NotNull
    public <R> OrderStep<T> orderBy(@NotNull Property<T, R> property) {
        return new OrderStep<>(this, PropertyNameResolver.resolve(property));
    }

    @NotNull
    public List<T> fetch() {
        Sort sort = buildSort();
        if (sort.isUnsorted()) {
            return executor.findAll(currentSpec());
        }
        return executor.findAll(currentSpec(), sort);
    }

    @NotNull
    public List<T> fetch(@Nullable Sort sort) {
        Sort combined = sort == null ? buildSort() : buildSort().and(sort);
        if (combined.isUnsorted()) {
            return executor.findAll(currentSpec());
        }
        return executor.findAll(currentSpec(), combined);
    }

    @NotNull
    public Page<T> fetch(@NotNull Pageable pageable) {
        Sort sort = buildSort();
        if (!sort.isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(sort));
        }
        return executor.findAll(currentSpec(), pageable);
    }

    @NotNull
    public Optional<T> fetchOne() {
        return executor.findOne(currentSpec());
    }

    public boolean exists() {
        Specification<T> base = spec;
        return executor.count(base) > 0;
    }

    protected void addCondition(Specification<T> newSpec, boolean isOr) {
        if (newSpec == null) {
            return;
        }
        if (spec == null) {
            spec = Specification.where(newSpec);
            return;
        }
        spec = isOr ? spec.or(newSpec) : spec.and(newSpec);
    }

    private Specification<T> currentSpec() {
        Specification<T> base = spec;
        if (!distinct && fetchJoins.isEmpty()) {
            return base;
        }
        return (root, query, cb) -> {
            if (distinct) {
                query.distinct(true);
            }
            if (!fetchJoins.isEmpty() && !isCountQuery(query)) {
                fetchJoins.forEach(fetch -> fetch.apply(root));
            }
            return base == null ? null : base.toPredicate(root, query, cb);
        };
    }

    void addOrder(@NotNull Sort.Order order) {
        this.orderings.add(order);
    }

    private Sort buildSort() {
        return orderings.isEmpty() ? Sort.unsorted() : Sort.by(orderings);
    }

    private FluentQuery<T> registerFetchJoin(String path, JoinType joinType) {
        Objects.requireNonNull(joinType, "JoinType must not be null");
        FetchJoinDescriptor descriptor = new FetchJoinDescriptor(path, joinType);
        fetchJoins.removeIf(existing -> existing.hasSamePath(descriptor.path()));
        fetchJoins.add(descriptor);
        return this;
    }

    private boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return resultType == Long.class || resultType == long.class;
    }
}
