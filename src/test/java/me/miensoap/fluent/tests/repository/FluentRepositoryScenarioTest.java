package me.miensoap.fluent.tests.repository;

import me.miensoap.fluent.support.AbstractFluentQueryIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import me.miensoap.fluent.support.PostFixtures;
import me.miensoap.fluent.support.PostFixtures.PostGraph;
import me.miensoap.fluent.support.entity.Member;
import me.miensoap.fluent.support.entity.MemberLikePost;
import me.miensoap.fluent.support.entity.Post;
import me.miensoap.fluent.support.repository.MemberLikePostRepository;
import me.miensoap.fluent.support.repository.PostRepository;

class FluentRepositoryScenarioTest extends AbstractFluentQueryIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberLikePostRepository likeRepository;

    private PostGraph posts;

    @BeforeEach
    void setUpGraph() {
        posts = PostFixtures.seedDefaultPosts(now, memberRepository, postRepository, likeRepository);
    }

    @Test
    @DisplayName("단일 getter 참조로 단순 필터링이 가능한지 검증한다")
    void filtersBySimpleGetterReference() {
        List<Member> active = memberRepository.query()
            .where(Member::getStatus).equalTo("ACTIVE")
            .fetch();

        assertThat(active)
            .allMatch(member -> "ACTIVE".equals(member.getStatus()));
    }

    @Test
    @DisplayName("boolean getter 와 체인을 혼합한 조건이 기대한 결과를 반환한다")
    void combinesBooleanGetterAndChain() {
        List<Member> devActives = memberRepository.query()
            .where(Member::getActive).isTrue()
            .and(member -> member.getTeam().getDepartmentCode()).equalTo("DEV")
            .fetch();

        assertThat(devActives)
            .allSatisfy(member -> {
                assertThat(member.getActive()).isTrue();
                assertThat(member.getTeam().getDepartmentCode()).isEqualTo("DEV");
            });
    }

    @Test
    @DisplayName("문자열 경로와 체인 프로퍼티를 혼합해도 동일한 결과를 얻는다")
    void mixesStringAndPropertyPaths() {
        List<Member> chained = memberRepository.query()
            .where(member -> member.getTeam().getDepartmentCode()).equalTo("OPS")
            .and(member -> member.getAddress().getCity()).equalTo("Seoul")
            .fetch();

        List<Member> stringBased = memberRepository.query()
            .where("team.departmentCode").equalTo("OPS")
            .and("address.city").equalTo("Seoul")
            .fetch();

        assertThat(ids(chained)).containsExactlyElementsOf(ids(stringBased));
    }

    @Test
    @DisplayName("fetchJoin 체인이 실제로 연관 그래프를 초기화한다")
    void fetchJoinChainEagerlyLoadsGraph() {
        entityManager.flush();
        entityManager.clear();
        clearExecutedSql();

        List<Post> opsAlerts = postRepository.query()
            .fetchJoin(Post::getAuthor)
            .fetchJoin(post -> post.getAuthor().getTeam())
            .where(Post::getId).equalTo(posts.opsAlert().getId())
            .fetch();

        assertThat(executedSql())
            .anyMatch(sql -> sql.toLowerCase().contains(" join ") && sql.toLowerCase().contains("member"));
        clearExecutedSql();

        assertThat(opsAlerts).hasSize(1);
        Post opsAlert = opsAlerts.get(0);
        assertThat(Hibernate.isInitialized(opsAlert.getAuthor())).isTrue();
        assertThat(Hibernate.isInitialized(opsAlert.getAuthor().getTeam())).isTrue();
        assertThat(executedSql()).isEmpty();
    }

    @Test
    @DisplayName("fetchJoin 이 추가된 상태에서도 exists() 가 안전하게 동작한다")
    void existsWithFetchJoinAvoidsHibernateError() {
        boolean exists = memberRepository.query()
            .fetchJoin(Member::getTeam)
            .where(Member::getStatus).equalTo("ACTIVE")
            .exists();

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("MemberLikePostRepository 도 체인 기반 쿼리를 안전하게 수행한다")
    void chainedQueryAgainstLikeRepository() {
        List<MemberLikePost> likes = likeRepository.query()
            .where(like -> like.getPost().getAuthor().getTeam().getDepartmentCode()).equalTo("OPS")
            .and(like -> like.getLiker().getTeam().getDepartmentCode()).equalTo("DEV")
            .fetch();

        assertThat(likes)
            .allSatisfy(like -> {
                assertThat(like.getPost().getAuthor().getTeam().getDepartmentCode()).isEqualTo("OPS");
                assertThat(like.getLiker().getTeam().getDepartmentCode()).isEqualTo("DEV");
            });
    }

    @Test
    @DisplayName("체인과 문자열 경로를 혼합한 복합 조건이 정확한 Post 를 찾는다")
    void mixedPropertyChainFiltersPosts() {
        List<Post> opsWithDevLikers = postRepository.query()
            .where(post -> post.getAuthor().getTeam().getDepartmentCode()).equalTo("OPS")
            .and("likes.liker.team.departmentCode").equalTo("DEV")
            .fetch();

        assertThat(opsWithDevLikers.stream().map(Post::getId).distinct().sorted().toList())
            .containsExactly(posts.opsAlert().getId(), posts.securityMemo().getId());
    }
}
