package com.fox.urlshortener.link;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.fox.urlshortener.auth.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByCode(String code);

    boolean existsByCode(String code);

    List<ShortLink> findAllByUserOrderByCreatedAtDesc(User user);

    List<ShortLink> findAllByUserAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(User user,
            Instant now);

    long countByUser(User user);

    long countByUserAndActiveTrueAndExpiresAtAfter(User user, Instant now);

    List<ShortLink> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<ShortLink> findAllByUserIdAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(Long userId,
            Instant now);

    @Query("""
            select link from ShortLink link
            join link.user owner
            where (:active is null or link.active = :active)
              and (:username is null or owner.username = :username)
              and (:expired is null or
                (:expired = true and link.expiresAt <= :now) or
                (:expired = false and link.expiresAt > :now))
            order by link.createdAt desc
            """)
    List<ShortLink> search(Boolean active, Boolean expired, String username, Instant now);
}
