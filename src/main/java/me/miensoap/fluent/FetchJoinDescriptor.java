package me.miensoap.fluent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.JoinType;

/**
 * Stores fetch join metadata and applies it to a Criteria query tree.
 */
final class FetchJoinDescriptor {

    private final String path;
    private final JoinType joinType;
    private final List<String> segments;

    FetchJoinDescriptor(String path, JoinType joinType) {
        Objects.requireNonNull(joinType, "JoinType must not be null");
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Fetch join path must not be blank");
        }
        this.path = path.trim();
        this.joinType = joinType;
        this.segments = parseSegments(this.path);
    }

    void apply(FetchParent<?, ?> parent) {
        FetchParent<?, ?> current = parent;
        for (String segment : segments) {
            current = fetch(current, segment);
        }
    }

    boolean hasSamePath(String candidate) {
        return path.equals(candidate == null ? null : candidate.trim());
    }

    String path() {
        return path;
    }

    @SuppressWarnings("unchecked")
    private FetchParent<?, ?> fetch(FetchParent<?, ?> parent, String attribute) {
        Fetch<?, ?> existing = findExisting(parent, attribute);
        if (existing != null) {
            return (FetchParent<?, ?>) existing;
        }
        return parent.fetch(attribute, joinType);
    }

    private Fetch<?, ?> findExisting(FetchParent<?, ?> parent, String attribute) {
        for (Fetch<?, ?> fetch : parent.getFetches()) {
            if (fetch.getAttribute() != null && attribute.equals(fetch.getAttribute().getName())) {
                return fetch;
            }
        }
        return null;
    }

    private List<String> parseSegments(String path) {
        String[] parts = path.split("\\.");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            String segment = part.trim();
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("Invalid fetch join path: " + path);
            }
            result.add(segment);
        }
        return result;
    }
}
