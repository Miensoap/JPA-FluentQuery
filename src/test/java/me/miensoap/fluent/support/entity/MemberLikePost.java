package me.miensoap.fluent.support.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberLikePost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member liker;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    private String reaction;
    private LocalDateTime likedAt;

    public MemberLikePost(Member liker, String reaction, LocalDateTime likedAt) {
        this.liker = liker;
        this.reaction = reaction;
        this.likedAt = likedAt;
    }

    void attachTo(Post post) {
        this.post = post;
    }
}
