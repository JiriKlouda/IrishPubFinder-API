package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.FeedItemDto;
import com.irishpubfinder.api.model.Friendship;
import com.irishpubfinder.api.model.Visit;
import com.irishpubfinder.api.repository.FriendshipRepository;
import com.irishpubfinder.api.repository.UserRepository;
import com.irishpubfinder.api.repository.VisitRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FeedService {

    private static final int MAX_VISITS_PER_FRIEND = 20;

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;

    public FeedService(FriendshipRepository friendshipRepository,
                       UserRepository userRepository,
                       VisitRepository visitRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.visitRepository = visitRepository;
    }

    public List<FeedItemDto> getFeed(String currentUserId) {
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(currentUserId);
        List<FeedItemDto> items = new ArrayList<>();

        for (Friendship friendship : friendships) {
            String friendId = friendship.getRequesterId().equals(currentUserId)
                    ? friendship.getAddresseeId()
                    : friendship.getRequesterId();

            userRepository.findById(friendId).ifPresent(friend -> {
                List<Visit> visits = visitRepository.findByUserIdOrderByCreatedAtDesc(friendId);
                int limit = Math.min(visits.size(), MAX_VISITS_PER_FRIEND);
                for (int i = 0; i < limit; i++) {
                    Visit v = visits.get(i);
                    items.add(new FeedItemDto(
                            friend.getId(),
                            friend.getEmail(),
                            friend.getDisplayName(),
                            v.getPlaceId(),
                            v.getName(),
                            v.getAddress(),
                            v.getLatitude(),
                            v.getLongitude(),
                            v.getPhotoUrl(),
                            v.getCreatedAt()
                    ));
                }
            });
        }

        items.sort(Comparator.comparing(FeedItemDto::getVisitedAt).reversed());
        return items;
    }
}
