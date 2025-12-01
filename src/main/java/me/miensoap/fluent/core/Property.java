package me.miensoap.fluent.core;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Serializable function for referencing an entity getter via method reference.
 */
@FunctionalInterface
public interface Property<T, R> extends Function<T, R>, Serializable {
}
