package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.Friendship;
import com.irishpubfinder.api.model.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE (f.requesterId = :userId OR f.addresseeId = :userId) AND f.status = com.irishpubfinder.api.model.FriendshipStatus.ACCEPTED")
    List<Friendship> findAcceptedFriendships(@Param("userId") String userId);

    List<Friendship> findByAddresseeIdAndStatus(String addresseeId, FriendshipStatus status);

    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE (f.requesterId = :a AND f.addresseeId = :b) OR (f.requesterId = :b AND f.addresseeId = :a)")
    boolean existsBetweenUsers(@Param("a") String a, @Param("b") String b);
}
