package me.miensoap.fluent.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String category;
    private boolean pinned;
    private String visibility;
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberLikePost> likes = new ArrayList<>();

    public Post(String title, String category, boolean pinned, String visibility,
                LocalDateTime publishedAt, Member author) {
        this.title = title;
        this.category = category;
        this.pinned = pinned;
        this.visibility = visibility;
        this.publishedAt = publishedAt;
        this.author = author;
    }

    public void addLike(MemberLikePost like) {
        like.attachTo(this);
        likes.add(like);
    }
}
