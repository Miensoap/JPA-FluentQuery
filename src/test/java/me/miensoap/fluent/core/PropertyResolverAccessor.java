package me.miensoap.fluent.core;

/**
 * Test-only bridge to access package-private resolver APIs.
 */
public final class PropertyResolverAccessor {

    private PropertyResolverAccessor() {
    }

    public static <T, R> String resolve(Property<T, R> property) {
        return PropertyNameResolver.resolve(property);
    }

    public static <T, R> Class<?> resolveType(Property<T, R> property) {
        return PropertyNameResolver.resolveType(property);
    }
}
