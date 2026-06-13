package com.irishpubfinder.api.service;

import com.irishpubfinder.api.repository.BadgeEventRepository;
import com.irishpubfinder.api.repository.FavouriteRepository;
import com.irishpubfinder.api.repository.FriendshipRepository;
import com.irishpubfinder.api.repository.GuinnessReviewRepository;
import com.irishpubfinder.api.repository.UserRepository;
import com.irishpubfinder.api.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final FavouriteRepository favouriteRepository;
    private final VisitRepository visitRepository;
    private final GuinnessReviewRepository guinnessReviewRepository;
    private final FriendshipRepository friendshipRepository;
    private final BadgeEventRepository badgeEventRepository;
    private final UserRepository userRepository;

    /**
     * Permanently deletes all data belonging to the user. Runs in a single transaction
     * so a partial failure rolls back cleanly. Child rows are removed before the user row
     * to satisfy FK constraints.
     */
    @Transactional
    public void deleteAccount(String userId) {
        favouriteRepository.deleteAllByUserId(userId);
        visitRepository.deleteAllByUserId(userId);
        guinnessReviewRepository.deleteAllByUserId(userId);
        friendshipRepository.deleteByRequesterIdOrAddresseeId(userId, userId);
        badgeEventRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }
}
