package me.miensoap.fluent.support;

import java.time.LocalDateTime;
import java.util.List;

import me.miensoap.fluent.support.entity.Member;
import me.miensoap.fluent.support.entity.MemberLikePost;
import me.miensoap.fluent.support.entity.Post;
import me.miensoap.fluent.support.repository.MemberLikePostRepository;
import me.miensoap.fluent.support.repository.MemberRepository;
import me.miensoap.fluent.support.repository.PostRepository;

public final class PostFixtures {

    private PostFixtures() {
    }

    public static PostGraph seedDefaultPosts(LocalDateTime now,
                                             MemberRepository memberRepository,
                                             PostRepository postRepository,
                                             MemberLikePostRepository likeRepository) {
        likeRepository.deleteAll();
        postRepository.deleteAll();

        Member alice = memberByEmail(memberRepository, "alice@corp.com");
        Member amanda = memberByEmail(memberRepository, "amanda@corp.com");
        Member bob = memberByEmail(memberRepository, "bob@corp.com");
        Member isaac = memberByEmail(memberRepository, "isaac@corp.com");
        Member sue = memberByEmail(memberRepository, "sue@partner.com");

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
        return new PostGraph(devDigest, opsAlert, partnershipLaunch, securityMemo);
    }

    private static Member memberByEmail(MemberRepository memberRepository, String email) {
        return memberRepository.findAll().stream()
            .filter(member -> email.equals(member.getEmail()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No member with email " + email));
    }

    public static final class PostGraph {

        private final Post devDigest;
        private final Post opsAlert;
        private final Post partnershipLaunch;
        private final Post securityMemo;

        PostGraph(Post devDigest, Post opsAlert, Post partnershipLaunch, Post securityMemo) {
            this.devDigest = devDigest;
            this.opsAlert = opsAlert;
            this.partnershipLaunch = partnershipLaunch;
            this.securityMemo = securityMemo;
        }

        public Post devDigest() {
            return devDigest;
        }

        public Post opsAlert() {
            return opsAlert;
        }

        public Post partnershipLaunch() {
            return partnershipLaunch;
        }

        public Post securityMemo() {
            return securityMemo;
        }
    }
}
