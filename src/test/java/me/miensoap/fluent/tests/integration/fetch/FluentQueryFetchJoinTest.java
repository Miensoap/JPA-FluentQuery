package me.miensoap.fluent.tests.integration.fetch;

import me.miensoap.fluent.support.AbstractFluentQueryIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import jakarta.persistence.criteria.JoinType;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import me.miensoap.fluent.support.entity.Member;
import me.miensoap.fluent.support.entity.MemberLikePost;
import me.miensoap.fluent.support.entity.Post;
import me.miensoap.fluent.support.repository.MemberLikePostRepository;
import me.miensoap.fluent.support.repository.PostRepository;
import me.miensoap.fluent.support.PostFixtures;
import me.miensoap.fluent.support.PostFixtures.PostGraph;

class FluentQueryFetchJoinTest extends AbstractFluentQueryIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberLikePostRepository likeRepository;

    @Test
    @DisplayName("fetchJoin(Member::getTeam) 으로 lazy 연관이 즉시 로딩된다")
    void fetchJoinLoadsLazyAssociation() {
        entityManager.flush();
        entityManager.clear();
        clearExecutedSql();

        List<Member> members = query()
            .fetchJoin(Member::getTeam)
            .fetch();

        List<String> sql = executedSql();
        assertThat(sql)
            .anyMatch(statement -> statement.toLowerCase().contains(" join ") && statement.toLowerCase().contains(" team "));

        clearExecutedSql();

        assertThatCode(() -> members.forEach(member -> member.getTeam().getName()))
            .doesNotThrowAnyException();
        assertThat(members)
            .allMatch(member -> Hibernate.isInitialized(member.getTeam()));
        assertThat(executedSql()).isEmpty();
    }

    @Test
    @DisplayName("fetchJoin(\"team\") 도 property 참조와 동일하게 동작한다")
    void fetchJoinSupportsStringPaths() {
        entityManager.flush();
        entityManager.clear();
        clearExecutedSql();

        List<Member> members = query()
            .fetchJoin("team")
            .fetch();

        assertThat(members)
            .allSatisfy(member -> assertThat(Hibernate.isInitialized(member.getTeam())).isTrue());
    }

    @Test
    @DisplayName("중복 경로 fetchJoin 은 마지막에 등록한 JoinType 으로 적용된다")
    void fetchJoinOverridesPreviousRegistration() {
        entityManager.flush();
        entityManager.clear();
        clearExecutedSql();

        query()
            .fetchJoin(Member::getTeam)
            .fetchJoin(Member::getTeam, JoinType.INNER)
            .fetch();

        List<String> sql = executedSql();
        assertThat(sql)
            .anyMatch(statement -> {
                String lowered = statement.toLowerCase();
                return lowered.contains(" join ")
                    && lowered.contains(" join team")
                    && !lowered.contains("left join team");
            });
    }

    @Test
    @DisplayName("fetchJoin() 은 count/exists 와 함께 사용해도 안전하다")
    void fetchJoinWithAggregateOperations() {
        long repositoryCount = memberRepository.count();
        clearExecutedSql();

        long counted = query()
            .fetchJoin(Member::getTeam)
            .count();

        List<String> sql = executedSql();
        assertThat(sql)
            .hasSize(1)
            .allMatch(statement -> !statement.toLowerCase().contains(" join "));
        assertThat(counted).isEqualTo(repositoryCount);
    }

    @Test
    @DisplayName("exists() 역시 fetch join 을 생략하고 최적화된 카운트만 수행한다")
    void fetchJoinSkipsForExists() {
        clearExecutedSql();

        boolean exists = query()
            .fetchJoin(Member::getTeam)
            .where(Member::getStatus).equalTo("ACTIVE")
            .exists();

        assertThat(exists).isTrue();
        assertThat(executedSql())
            .hasSize(1)
            .allMatch(statement -> !statement.toLowerCase().contains(" join "));
    }

    @Test
    @DisplayName("컬렉션 fetch join 결과도 distinct() 로 기준 엔티티 수를 유지한다")
    void collectionFetchJoinWorksWithDistinct() {
        entityManager.flush();
        entityManager.clear();
        clearExecutedSql();

        List<Member> deduped = query()
            .fetchJoin(Member::getTags)
            .distinct()
            .fetch();

        List<String> sql = executedSql();
        assertThat(sql)
            .anyMatch(statement -> statement.toLowerCase().contains(" join ") && statement.toLowerCase().contains("member_tags"));
        clearExecutedSql();

        assertThat(deduped).hasSize((int) memberRepository.count());
        assertThat(ids(deduped)).isEqualTo(ids(memberRepository.findAll()));
    }

    @Test
    @DisplayName("fetchJoin(Post::getLikes) 와 likes.liker 체인을 함께 조회할 수 있다")
    void fetchJoinSupportsNestedCollectionPaths() {
        PostGraph graph = PostFixtures.seedDefaultPosts(now, memberRepository, postRepository, likeRepository);
        entityManager.flush();
        entityManager.clear();
        clearExecutedSql();

        List<Post> posts = postRepository.query()
            .fetchJoin(Post::getLikes)
            .fetchJoin("likes.liker")
            .fetchJoin("likes.liker.team", JoinType.LEFT)
            .where(Post::getId).equalTo(graph.opsAlert().getId())
            .fetch();

        assertThat(posts).hasSize(1);
        Post opsAlert = posts.get(0);
        assertThat(Hibernate.isInitialized(opsAlert.getLikes())).isTrue();
        for (MemberLikePost like : opsAlert.getLikes()) {
            assertThat(Hibernate.isInitialized(like.getLiker())).isTrue();
            assertThat(Hibernate.isInitialized(like.getLiker().getTeam())).isTrue();
        }

        List<String> sql = executedSql();
        assertThat(sql)
            .anyMatch(statement -> statement.toLowerCase().contains("member_like_post"));
    }
}