package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.BadgeEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BadgeEventRepository extends JpaRepository<BadgeEvent, Long> {

    boolean existsByUserIdAndBadgeId(String userId, String badgeId);

    long countByUserId(String userId);

    @Query("SELECT b FROM BadgeEvent b WHERE b.userId IN :userIds ORDER BY b.earnedAt DESC")
    List<BadgeEvent> findByUserIdInOrderByEarnedAtDesc(@Param("userIds") List<String> userIds);

    @Query("SELECT b FROM BadgeEvent b WHERE b.userId IN :userIds AND b.earnedAt < :before ORDER BY b.earnedAt DESC")
    List<BadgeEvent> findPageByUserIds(@Param("userIds") List<String> userIds,
                                       @Param("before") LocalDateTime before,
                                       Pageable pageable);

    void deleteAllByUserId(String userId);
}
