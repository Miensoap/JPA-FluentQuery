### Property chaining

```java
        var likes = likePostRepository.query()
                .and(like -> like.getPost().getId())
                .equalTo(posetId)
                .fetchJoin(LikePost::getMember)
                .fetch();
```

### Projection

```java


```
