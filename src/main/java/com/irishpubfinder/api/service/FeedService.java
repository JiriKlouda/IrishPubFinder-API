package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.FeedItemDto;
import com.irishpubfinder.api.model.BadgeEvent;
import com.irishpubfinder.api.model.Friendship;
import com.irishpubfinder.api.model.User;
import com.irishpubfinder.api.model.Visit;
import com.irishpubfinder.api.repository.BadgeEventRepository;
import com.irishpubfinder.api.repository.FriendshipRepository;
import com.irishpubfinder.api.repository.UserRepository;
import com.irishpubfinder.api.repository.VisitRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;
    private final BadgeEventRepository badgeEventRepository;

    public FeedService(FriendshipRepository friendshipRepository,
                       UserRepository userRepository,
                       VisitRepository visitRepository,
                       BadgeEventRepository badgeEventRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.visitRepository = visitRepository;
        this.badgeEventRepository = badgeEventRepository;
    }

    public List<FeedItemDto> getFeed(String currentUserId, LocalDateTime before, int size) {
        // Collect self + friend IDs
        List<String> allUserIds = new ArrayList<>();
        allUserIds.add(currentUserId);

        for (Friendship f : friendshipRepository.findAcceptedFriendships(currentUserId)) {
            String friendId = f.getRequesterId().equals(currentUserId)
                    ? f.getAddresseeId() : f.getRequesterId();
            allUserIds.add(friendId);
        }

        // Load users once for lookup
        Map<String, User> userMap = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Fetch one page worth from each source — any item in the true top-N
        // must appear in the top-N of its own source, so fetching N from each is correct.
        var pageable = PageRequest.of(0, size);

        List<Visit> visits = visitRepository.findPageByUserIds(allUserIds, before, pageable);
        List<BadgeEvent> badges = badgeEventRepository.findPageByUserIds(allUserIds, before, pageable);

        List<FeedItemDto> items = new ArrayList<>();

        for (Visit v : visits) {
            User user = userMap.get(v.getUserId());
            if (user != null) items.add(FeedItemDto.visitEvent(user, v));
        }

        for (BadgeEvent be : badges) {
            User user = userMap.get(be.getUserId());
            if (user != null) items.add(FeedItemDto.badgeEvent(user, be));
        }

        items.sort(Comparator.comparing(FeedItemDto::getVisitedAt).reversed());

        // Trim to exactly `size` after interleaving the two sources
        return items.size() > size ? items.subList(0, size) : items;
    }
}
