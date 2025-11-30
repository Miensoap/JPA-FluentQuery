package me.miensoap.fluent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Builder that composes Specifications through a fluent API.
 */
public class FluentQuery<T> {

    private final JpaSpecificationExecutor<T> executor;
    private Specification<T> spec = Specification.where(null);
    private boolean distinct;
    private final List<Sort.Order> orderings = new ArrayList<>();

    public FluentQuery(JpaSpecificationExecutor<T> executor) {
        this.executor = executor;
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

    public FieldStep<T> where(String field) {
        return new FieldStep<>(this, field, false);
    }

    public <R> FieldStep<T> where(Property<T, R> property) {
        String field = PropertyNameResolver.resolve(property);
        return new FieldStep<>(this, field, false, PropertyNameResolver.resolveType(property));
    }

    public FluentQuery<T> where(Specification<T> specification) {
        this.spec = Specification.where(specification);
        return this;
    }

    public FieldStep<T> and(String field) {
        return new FieldStep<>(this, field, false);
    }

    public <R> FieldStep<T> and(Property<T, R> property) {
        String field = PropertyNameResolver.resolve(property);
        return new FieldStep<>(this, field, false, PropertyNameResolver.resolveType(property));
    }

    public FieldStep<T> or(String field) {
        return new FieldStep<>(this, field, true);
    }

    public <R> FieldStep<T> or(Property<T, R> property) {
        String field = PropertyNameResolver.resolve(property);
        return new FieldStep<>(this, field, true, PropertyNameResolver.resolveType(property));
    }

    public OrderStep<T> orderBy(String field) {
        return new OrderStep<>(this, field);
    }

    public <R> OrderStep<T> orderBy(Property<T, R> property) {
        return new OrderStep<>(this, PropertyNameResolver.resolve(property));
    }

    public FluentQuery<T> and(Specification<T> specification) {
        addCondition(specification, false);
        return this;
    }

    public FluentQuery<T> or(Specification<T> specification) {
        addCondition(specification, true);
        return this;
    }

    public FluentQuery<T> not() {
        if (spec != null) {
            spec = Specification.not(spec);
        }
        return this;
    }

    public FluentQuery<T> distinct() {
        this.distinct = true;
        return this;
    }

    public List<T> fetch() {
        Sort sort = buildSort();
        if (sort.isUnsorted()) {
            return executor.findAll(currentSpec());
        }
        return executor.findAll(currentSpec(), sort);
    }

    public List<T> fetch(Sort sort) {
        Sort combined = sort == null ? buildSort() : buildSort().and(sort);
        if (combined.isUnsorted()) {
            return executor.findAll(currentSpec());
        }
        return executor.findAll(currentSpec(), combined);
    }

    public Page<T> fetch(Pageable pageable) {
        Sort sort = buildSort();
        if (!sort.isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(sort));
        }
        return executor.findAll(currentSpec(), pageable);
    }

    public Optional<T> fetchOne() {
        return executor.findOne(currentSpec());
    }

    public long count() {
        return executor.count(currentSpec());
    }

    public boolean exists() {
        return executor.exists(currentSpec());
    }

    private Specification<T> currentSpec() {
        if (!distinct) {
            return spec;
        }
        Specification<T> base = spec;
        return (root, query, cb) -> {
            query.distinct(true);
            return base == null ? null : base.toPredicate(root, query, cb);
        };
    }

    void addOrder(Sort.Order order) {
        this.orderings.add(order);
    }

    private Sort buildSort() {
        return orderings.isEmpty() ? Sort.unsorted() : Sort.by(orderings);
    }
}
