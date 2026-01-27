package me.miensoap.fluent.core;

import java.util.Collection;
import java.util.Objects;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Field-scoped operations that translate fluent calls to Specifications.
 */
public class FieldStep<T> {

    private final FluentQuery<T> builder;
    private final String field;
    private final boolean isOr;
    private final Class<?> propertyType;
    private final boolean typed;

    FieldStep(FluentQuery<T> builder, String field, boolean isOr) {
        this(builder, field, isOr, null, false);
    }

    FieldStep(FluentQuery<T> builder, String field, boolean isOr, Class<?> propertyType) {
        this(builder, field, isOr, propertyType, true);
    }

    private FieldStep(FluentQuery<T> builder, String field, boolean isOr, Class<?> propertyType, boolean typed) {
        this.builder = builder;
        this.field = field;
        this.isOr = isOr;
        this.propertyType = propertyType;
        this.typed = typed;
    }

    @NotNull
    public FluentQuery<T> equalTo(@Nullable Object value) {
        return apply((root, query, cb) -> cb.equal(path(root), value));
    }

    @NotNull
    public FluentQuery<T> notEqualTo(@Nullable Object value) {
        return apply((root, query, cb) -> cb.notEqual(path(root), value));
    }

    @NotNull
    public FluentQuery<T> greaterThan(@NotNull Number value) {
        ensureNumber("greaterThan");
        requireValue(value, "greaterThan");
        return apply((root, query, cb) -> cb.gt(path(root, Number.class), value));
    }

    @NotNull
    public FluentQuery<T> greaterThanOrEqualTo(@NotNull Number value) {
        ensureNumber("greaterThanOrEqualTo");
        requireValue(value, "greaterThanOrEqualTo");
        return apply((root, query, cb) -> cb.ge(path(root, Number.class), value));
    }

    @NotNull
    public FluentQuery<T> lessThan(@NotNull Number value) {
        ensureNumber("lessThan");
        requireValue(value, "lessThan");
        return apply((root, query, cb) -> cb.lt(path(root, Number.class), value));
    }

    @NotNull
    public FluentQuery<T> lessThanOrEqualTo(@NotNull Number value) {
        ensureNumber("lessThanOrEqualTo");
        requireValue(value, "lessThanOrEqualTo");
        return apply((root, query, cb) -> cb.le(path(root, Number.class), value));
    }

    @NotNull
    public <Y extends Comparable<? super Y>> FluentQuery<T> between(@NotNull Y start, @NotNull Y end) {
        ensureComparable("between");
        requireValue(start, "between start");
        requireValue(end, "between end");
        return apply((root, query, cb) -> cb.between(path(root), start, end));
    }

    @NotNull
    public <Y extends Comparable<? super Y>> FluentQuery<T> after(@NotNull Y value) {
        ensureComparable("after");
        requireValue(value, "after");
        return apply((root, query, cb) -> cb.greaterThan(path(root), value));
    }

    @NotNull
    public <Y extends Comparable<? super Y>> FluentQuery<T> before(@NotNull Y value) {
        ensureComparable("before");
        requireValue(value, "before");
        return apply((root, query, cb) -> cb.lessThan(path(root), value));
    }

    @NotNull
    public FluentQuery<T> like(@NotNull String pattern) {
        ensureString("like");
        requireText(pattern, "like");
        return apply((root, query, cb) -> cb.like(path(root, String.class), pattern));
    }

    @NotNull
    public FluentQuery<T> containing(@NotNull String value) {
        ensureString("containing");
        requireText(value, "containing");
        return apply((root, query, cb) -> cb.like(path(root, String.class), "%" + value + "%"));
    }

    @NotNull
    public FluentQuery<T> startingWith(@NotNull String value) {
        ensureString("startingWith");
        requireText(value, "startingWith");
        return apply((root, query, cb) -> cb.like(path(root, String.class), value + "%"));
    }

    @NotNull
    public FluentQuery<T> endingWith(@NotNull String value) {
        ensureString("endingWith");
        requireText(value, "endingWith");
        return apply((root, query, cb) -> cb.like(path(root, String.class), "%" + value));
    }

    @NotNull
    public FluentQuery<T> notContaining(@NotNull String value) {
        ensureString("notContaining");
        requireText(value, "notContaining");
        return apply((root, query, cb) -> cb.notLike(path(root, String.class), "%" + value + "%"));
    }

    @NotNull
    public FluentQuery<T> notLike(@NotNull String pattern) {
        ensureString("notLike");
        requireText(pattern, "notLike");
        return apply((root, query, cb) -> cb.notLike(path(root, String.class), pattern));
    }

    @NotNull
    public FluentQuery<T> likeIgnoreCase(@NotNull String pattern) {
        ensureString("likeIgnoreCase");
        requireText(pattern, "likeIgnoreCase");
        return apply((root, query, cb) -> cb.like(cb.lower(path(root, String.class)), pattern.toLowerCase()));
    }

    @NotNull
    public FluentQuery<T> containingIgnoreCase(@NotNull String value) {
        ensureString("containingIgnoreCase");
        requireText(value, "containingIgnoreCase");
        return apply((root, query, cb) -> cb.like(cb.lower(path(root, String.class)), ("%" + value + "%").toLowerCase()));
    }

    @NotNull
    public FluentQuery<T> startingWithIgnoreCase(@NotNull String value) {
        ensureString("startingWithIgnoreCase");
        requireText(value, "startingWithIgnoreCase");
        return apply((root, query, cb) -> cb.like(cb.lower(path(root, String.class)), (value + "%").toLowerCase()));
    }

    @NotNull
    public FluentQuery<T> endingWithIgnoreCase(@NotNull String value) {
        ensureString("endingWithIgnoreCase");
        requireText(value, "endingWithIgnoreCase");
        return apply((root, query, cb) -> cb.like(cb.lower(path(root, String.class)), ("%" + value).toLowerCase()));
    }

    @NotNull
    public FluentQuery<T> in(@NotNull Collection<?> values) {
        Collection<?> normalized = requireCollection(values, "in");
        if (normalized.isEmpty()) {
            return apply((root, query, cb) -> cb.disjunction());
        }
        return apply((root, query, cb) -> path(root).in(normalized));
    }

    @NotNull
    public FluentQuery<T> notIn(@NotNull Collection<?> values) {
        Collection<?> normalized = requireCollection(values, "notIn");
        if (normalized.isEmpty()) {
            return apply((root, query, cb) -> cb.conjunction());
        }
        return apply((root, query, cb) -> cb.not(path(root).in(normalized)));
    }

    @NotNull
    public FluentQuery<T> isNull() {
        return apply((root, query, cb) -> cb.isNull(path(root)));
    }

    @NotNull
    public FluentQuery<T> isNotNull() {
        return apply((root, query, cb) -> cb.isNotNull(path(root)));
    }

    @NotNull
    public FluentQuery<T> isTrue() {
        ensureBoolean("isTrue");
        return apply((root, query, cb) -> cb.isTrue(path(root, Boolean.class)));
    }

    @NotNull
    public FluentQuery<T> isFalse() {
        ensureBoolean("isFalse");
        return apply((root, query, cb) -> cb.isFalse(path(root, Boolean.class)));
    }

    @NotNull
    public FluentQuery<T> isEmpty() {
        ensureCollection("isEmpty");
        return apply((root, query, cb) -> cb.isEmpty(path(root)));
    }

    @NotNull
    public FluentQuery<T> isNotEmpty() {
        ensureCollection("isNotEmpty");
        return apply((root, query, cb) -> cb.isNotEmpty(path(root)));
    }

    private FluentQuery<T> apply(Specification<T> next) {
        builder.addCondition(next, isOr);
        return builder;
    }

    @SuppressWarnings("unchecked")
    private <Y> Path<Y> path(Root<T> root) {
        Path<?> current = root;
        for (String part : field.split("\\.")) {
            current = current.get(part);
        }
        return (Path<Y>) current;
    }

    @SuppressWarnings("unchecked")
    private <Y> Path<Y> path(Root<T> root, Class<Y> type) {
        return path(root);
    }

    private void requireValue(Object value, String name) {
        Objects.requireNonNull(value, name + " value must not be null");
    }

    private void requireText(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " value must not be null");
        }
    }

    private Collection<?> requireCollection(Collection<?> values, String name) {
        if (values == null) {
            throw new IllegalArgumentException(name + " collection must not be null");
        }
        return values;
    }

    private void ensureNumber(String operation) {
        if (typed && !Number.class.isAssignableFrom(boxed(propertyType))) {
            throw new IllegalArgumentException(operation + " is only supported for numeric properties but was " + describeType());
        }
    }

    private void ensureComparable(String operation) {
        if (typed && !Comparable.class.isAssignableFrom(boxed(propertyType))) {
            throw new IllegalArgumentException(operation + " requires Comparable property but was " + describeType());
        }
    }

    private void ensureString(String operation) {
        if (typed && !CharSequence.class.isAssignableFrom(boxed(propertyType))) {
            throw new IllegalArgumentException(operation + " requires a String property but was " + describeType());
        }
    }

    private void ensureBoolean(String operation) {
        if (typed && boxed(propertyType) != Boolean.class) {
            throw new IllegalArgumentException(operation + " requires a Boolean property but was " + describeType());
        }
    }

    private void ensureCollection(String operation) {
        if (typed && !java.util.Collection.class.isAssignableFrom(boxed(propertyType))) {
            throw new IllegalArgumentException(operation + " requires a Collection property but was " + describeType());
        }
    }

    private Class<?> boxed(Class<?> type) {
        if (type == null) {
            return Object.class;
        }
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == Integer.TYPE) return Integer.class;
        if (type == Long.TYPE) return Long.class;
        if (type == Short.TYPE) return Short.class;
        if (type == Byte.TYPE) return Byte.class;
        if (type == Double.TYPE) return Double.class;
        if (type == Float.TYPE) return Float.class;
        if (type == Boolean.TYPE) return Boolean.class;
        if (type == Character.TYPE) return Character.class;
        return type;
    }

    private String describeType() {
        if (propertyType == null) {
            return field + " (unknown type)";
        }
        return field + " (" + boxed(propertyType).getTypeName() + ")";
    }
}
