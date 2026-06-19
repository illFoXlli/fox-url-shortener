package com.fox.urlshortener.link.repository;

import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.link.model.ShortLink;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            select link from ShortLink link
            where link.code = :code
              and link.active = true
              and link.expiresAt > :now
            """)
    Optional<ShortLink> findActiveByCode(@Param("code") String code, @Param("now") Instant now);

    List<ShortLink> findAllByUserOrderByCreatedAtDesc(User user);

    List<ShortLink> findAllByUserAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(User user,
            Instant now);

    long countByUser(User user);

    long countByUserAndActiveTrueAndExpiresAtAfter(User user, Instant now);

    List<ShortLink> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<ShortLink> findAllByUserIdAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(Long userId,
            Instant now);

    @Modifying
    @Query("""
            update ShortLink link
            set link.clickCount = link.clickCount + 1,
                link.updatedAt = :now
            where link.code = :code
              and link.active = true
              and link.expiresAt > :now
            """)
    int incrementClickCount(@Param("code") String code, @Param("now") Instant now);

    @Modifying
    @Query("""
            update ShortLink link
            set link.clickCount = link.clickCount + :amount,
                link.updatedAt = :now
            where link.code = :code
            """)
    int addClickCount(@Param("code") String code, @Param("amount") long amount,
            @Param("now") Instant now);

    @Query("""
            select link from ShortLink link
            join link.user owner
            where (:active is null or link.active = :active)
              and (:login is null or owner.login = :login)
              and (:expired is null or
                (:expired = true and link.expiresAt <= :now) or
                (:expired = false and link.expiresAt > :now))
            order by link.createdAt desc
            """)
    List<ShortLink> search(Boolean active, Boolean expired, String login, Instant now);
}
