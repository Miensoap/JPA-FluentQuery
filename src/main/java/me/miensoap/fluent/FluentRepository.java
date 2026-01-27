package me.miensoap.fluent;

import me.miensoap.fluent.core.FluentQuery;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repository mixin that exposes the fluent query builder entry point.
 */
@NoRepositoryBean
public interface FluentRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    @NotNull
    default FluentQuery<T> query() {
        return new FluentQuery<>(this);
    }
}
