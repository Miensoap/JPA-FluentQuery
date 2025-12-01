package me.miensoap.fluent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import me.miensoap.fluent.entity.Member;
import me.miensoap.fluent.entity.MemberLikePost;
import me.miensoap.fluent.entity.MembershipType;
import me.miensoap.fluent.entity.Post;
import me.miensoap.fluent.repository.PostRepository;

/**
 * Member ───< Post ───< MemberLikePost 와 MemberLikePost >─── Member (liker) 관계를 그대로 사용한다.
 *
 * Member (author) 1 ───< Post (N)
 * Post (1) ───< MemberLikePost (N)
 * MemberLikePost (N) >───1 Member (liker)
 */
class FluentQueryReadabilityShowcaseTest extends AbstractFluentQueryIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUpPosts() {
        postRepository.deleteAll();

        Member alice = memberByEmail("alice@corp.com");
        Member amanda = memberByEmail("amanda@corp.com");
        Member bob = memberByEmail("bob@corp.com");
        Member isaac = memberByEmail("isaac@corp.com");
        Member sue = memberByEmail("sue@partner.com");

        Post opsAlert = new Post(
            "Redis 장애 전파",
            "OPS_ALERT",
            true,
            "INTERNAL",
            now.minusHours(1),
            bob
        );
        opsAlert.addLike(new MemberLikePost(alice, "UPVOTE", now.minusMinutes(30)));
        opsAlert.addLike(new MemberLikePost(amanda, "UPVOTE", now.minusMinutes(15)));

        Post partnershipLaunch = new Post(
            "파트너 런칭 공지",
            "PARTNER",
            false,
            "PUBLIC",
            now.minusDays(1),
            sue
        );
        partnershipLaunch.addLike(new MemberLikePost(alice, "CELEBRATE", now.minusHours(5)));

        Post devDigest = new Post(
            "개발 주간 다이제스트",
            "DEV",
            false,
            "PUBLIC",
            now.minusHours(3),
            alice
        );
        devDigest.addLike(new MemberLikePost(amanda, "BOOKMARK", now.minusHours(2)));
        devDigest.addLike(new MemberLikePost(bob, "UPVOTE", now.minusHours(1)));

        Post securityMemo = new Post(
            "보안 모범사례",
            "SECURITY",
            true,
            "INTERNAL",
            now.minusDays(2),
            isaac
        );
        securityMemo.addLike(new MemberLikePost(alice, "ACK", now.minusDays(2).plusHours(1)));

        postRepository.saveAll(List.of(opsAlert, partnershipLaunch, devDigest, securityMemo));
    }

    @Test
    @DisplayName("운영 알림 피드를 문장형 DSL 로 표현하면 가독성이 획기적으로 좋아진다")
    void opsAlertFeedReadsLikeSentence() {
        List<Post> opsFeed = postRepository.query()
            .where(Post::getCategory).equalTo("OPS_ALERT")
            .and(Post::isPinned).isTrue()
            .and(Post::getVisibility).notEqualTo("PUBLIC")
            .and("author.team.departmentCode").equalTo("OPS")
            .fetch();

        assertThat(titles(opsFeed)).containsExactly("Redis 장애 전파");
    }

    @Test
    @DisplayName("VIP 추천 영역의 복잡한 OR/AND 조합도 한 눈에 읽힌다")
    void vipRecommendationFilterRemainsReadable() {
        Specification<Post> pinnedOpsOrSecurity = (root, query, cb) -> cb.and(
            cb.isTrue(root.get("pinned")),
            root.get("category").in(List.of("OPS_ALERT", "SECURITY"))
        );

        List<Post> curated = postRepository.query()
            .where(Post::getVisibility).equalTo("PUBLIC")
            .and(Post::getPublishedAt).after(now.minusDays(2))
            .and("likes.liker.membershipType").in(List.of(MembershipType.VIP, MembershipType.VIP_GOLD))
            .or(pinnedOpsOrSecurity)
            .distinct()
            .orderBy(Post::getPublishedAt).descending()
            .fetch();

        assertThat(titles(curated)).containsExactly("Redis 장애 전파", "파트너 런칭 공지", "보안 모범사례");
    }

    @Test
    @DisplayName("fetchJoin 체인으로 member-post-like 그래프를 그대로 조회한다")
    void fetchJoinBringsEntireRelationshipGraph() {
        entityManager.flush();
        entityManager.clear();

        List<Post> devDigest = postRepository.query()
            .fetchJoin(Post::getAuthor)
            .fetchJoin("likes.liker")
            .where(Post::getCategory).equalTo("DEV")
            .fetch();

        assertThat(devDigest).hasSize(1);
        assertThat(devDigest.get(0).getAuthor().getEmail()).isEqualTo("alice@corp.com");
        assertThat(devDigest.get(0).getLikes())
            .allSatisfy(like -> assertThat(Hibernate.isInitialized(like.getLiker())).isTrue());
    }

    @Test
    @DisplayName("포스트 피드를 최신순으로 페이징하며 좋아요 메타데이터도 함께 계산한다")
    void pagedFeedCarriesReactionMetadata() {
        Member viewer = memberByEmail("alice@corp.com");

        var page = postRepository.query()
            .where(Post::getVisibility).equalTo("PUBLIC")
            .orderBy(Post::getPublishedAt).descending()
            .fetch(PageRequest.of(0, 2));

        assertThat(titles(page.getContent())).containsExactly("개발 주간 다이제스트", "파트너 런칭 공지");

        List<Boolean> likedFlags = page.getContent().stream()
            .map(post -> post.getLikes().stream().anyMatch(like -> like.getLiker().getId().equals(viewer.getId())))
            .collect(Collectors.toList());

        List<Integer> likeCounts = page.getContent().stream()
            .map(post -> post.getLikes().size())
            .collect(Collectors.toList());

        assertThat(likedFlags).containsExactly(false, true);
        assertThat(likeCounts).containsExactly(2, 1);
    }

    private Member memberByEmail(String email) {
        return memberRepository.findAll().stream()
            .filter(member -> email.equals(member.getEmail()))
            .findFirst()
            .orElseThrow();
    }

    private List<String> titles(List<Post> posts) {
        return posts.stream()
            .map(Post::getTitle)
            .collect(Collectors.toList());
    }
}
