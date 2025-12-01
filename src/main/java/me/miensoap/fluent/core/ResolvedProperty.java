package me.miensoap.fluent.core;

final class ResolvedProperty {

    private final String path;
    private final Class<?> type;

    ResolvedProperty(String path, Class<?> type) {
        this.path = path;
        this.type = type;
    }

    String path() {
        return path;
    }

    Class<?> type() {
        return type;
    }
}
