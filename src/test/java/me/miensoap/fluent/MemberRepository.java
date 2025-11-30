package me.miensoap.fluent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends
    FluentRepository<Member, Long> {

    List<Member> findByStatusAndAgeGreaterThanEqual(String status, Integer age);

    List<Member> findByGradeEndingWith(String suffix);

    List<Member> findByStatusNotContaining(String fragment);

    List<Member> findByRoleNotIn(Collection<String> roles);

    List<Member> findByTagsIsEmpty();

    Optional<Member> findFirstByRoleOrderByIdAsc(String role);

    boolean existsByRole(String role);

    long countByStatus(String status);
}
