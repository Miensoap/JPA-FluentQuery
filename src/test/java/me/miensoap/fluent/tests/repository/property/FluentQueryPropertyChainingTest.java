package me.miensoap.fluent.tests.repository.property;

import me.miensoap.fluent.support.AbstractFluentQueryIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import me.miensoap.fluent.support.entity.MemberLikePost;
import me.miensoap.fluent.support.entity.Post;
import me.miensoap.fluent.support.repository.MemberLikePostRepository;
import me.miensoap.fluent.support.repository.PostRepository;
import me.miensoap.fluent.support.PostFixtures;
import me.miensoap.fluent.support.PostFixtures.PostGraph;

class FluentQueryPropertyChainingTest extends AbstractFluentQueryIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberLikePostRepository likeRepository;

    private Post devDigest;
    private Post opsAlert;
    private Post partnershipLaunch;

    @BeforeEach
    void setUpGraph() {
        PostGraph graph = PostFixtures.seedDefaultPosts(now, memberRepository, postRepository, likeRepository);
        devDigest = graph.devDigest();
        opsAlert = graph.opsAlert();
        partnershipLaunch = graph.partnershipLaunch();
    }

    @Test
    @DisplayName("1 depth 연관관계의 식별자를 체이닝으로 비교할 수 있다")
    void filtersByPostIdThroughChainedProperty() {
        List<MemberLikePost> likes = likeRepository.query()
            .where(like -> like.getPost().getId()).equalTo(devDigest.getId())
            .fetch();

        assertThat(likes)
            .hasSize(2)
            .allSatisfy(like -> assertThat(like.getPost().getId()).isEqualTo(devDigest.getId()));
    }

    @Test
    @DisplayName("여러 단계 연관관계 체이닝으로 팀 코드까지 비교할 수 있다")
    void filtersByDeeplyNestedAssociation() {
        List<MemberLikePost> opsLikes = likeRepository.query()
            .where(like -> like.getPost().getAuthor().getTeam().getDepartmentCode()).equalTo("OPS")
            .fetch();

        assertThat(opsLikes)
            .hasSize(3)
            .allSatisfy(like -> assertThat(like.getPost().getAuthor().getTeam().getDepartmentCode()).isEqualTo("OPS"));
    }

    @Test
    @DisplayName("필수 fetch join 체이닝으로 지연 로딩 없이 그래프를 탐색한다")
    void fetchJoinChainInitializesRequiredGraph() {
        entityManager.flush();
        entityManager.clear();

        List<MemberLikePost> likes = likeRepository.query()
            .fetchJoin(like -> like.getPost().getAuthor())
            .fetchJoin(like -> like.getLiker().getTeam())
            .where(like -> like.getPost().getCategory()).equalTo("OPS_ALERT")
            .fetch();

        assertThat(likes).hasSize(2);
        likes.forEach(like -> {
            assertThat(Hibernate.isInitialized(like.getPost().getAuthor())).isTrue();
            assertThat(Hibernate.isInitialized(like.getLiker().getTeam())).isTrue();
        });
    }

    @Test
    @DisplayName("getter 체인이 아닌 메서드를 호출하면 즉시 친절한 예외로 실패한다")
    void failsFastWhenNonGetterMethodIsInvoked() {
        assertThatThrownBy(() -> likeRepository.query()
            .where(like -> like.getPost().getTitle().toLowerCase())
            .equalTo("dummy"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("getter chain")
            .hasMessageContaining("post.title");
    }

}