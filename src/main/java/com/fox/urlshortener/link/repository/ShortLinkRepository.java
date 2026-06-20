package com.fox.urlshortener.link.repository;

import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.link.model.ShortLink;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<ShortLink> findAllByUser(User user, Pageable pageable);

    Page<ShortLink> findAllByUserAndActiveTrueAndExpiresAtAfter(User user, Instant now,
            Pageable pageable);

    long countByUser(User user);

    long countByUserAndActiveTrueAndExpiresAtAfter(User user, Instant now);

    List<ShortLink> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Page<ShortLink> findAllByUserId(Long userId, Pageable pageable);

    Page<ShortLink> findAllByUserIdAndActiveTrueAndExpiresAtAfter(Long userId, Instant now,
            Pageable pageable);

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

    @Query(value = """
            select link from ShortLink link
            join link.user owner
            where (:active is null or link.active = :active)
              and (:login is null or owner.login = :login)
              and (:expired is null or
                (:expired = true and link.expiresAt <= :now) or
                (:expired = false and link.expiresAt > :now))
            """, countQuery = """
            select count(link) from ShortLink link
            join link.user owner
            where (:active is null or link.active = :active)
              and (:login is null or owner.login = :login)
              and (:expired is null or
                (:expired = true and link.expiresAt <= :now) or
                (:expired = false and link.expiresAt > :now))
            """)
    Page<ShortLink> search(
            @Param("active") Boolean active,
            @Param("expired") Boolean expired,
            @Param("login") String login,
            @Param("now") Instant now,
            Pageable pageable);
}
