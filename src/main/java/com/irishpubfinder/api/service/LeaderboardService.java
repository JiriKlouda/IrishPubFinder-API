package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.CoordDto;
import com.irishpubfinder.api.dto.HeadToHeadDto;
import com.irishpubfinder.api.dto.HeadToHeadEntry;
import com.irishpubfinder.api.dto.LeaderboardEntry;
import com.irishpubfinder.api.model.Friendship;
import com.irishpubfinder.api.model.User;
import com.irishpubfinder.api.model.Visit;
import com.irishpubfinder.api.repository.BadgeEventRepository;
import com.irishpubfinder.api.repository.FavouriteRepository;
import com.irishpubfinder.api.repository.FriendshipRepository;
import com.irishpubfinder.api.repository.GuinnessReviewRepository;
import com.irishpubfinder.api.repository.UserRepository;
import com.irishpubfinder.api.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;
    private final FavouriteRepository favouriteRepository;
    private final GuinnessReviewRepository guinnessReviewRepository;
    private final BadgeService badgeService;
    private final BadgeEventRepository badgeEventRepository;

    public List<LeaderboardEntry> getLeaderboard(String currentUserId) {
        User currentUser = userRepository.findById(currentUserId).orElseThrow();
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(currentUserId);

        List<LeaderboardEntry> entries = new ArrayList<>();
        entries.add(buildEntry(currentUser, true));

        for (Friendship f : friendships) {
            String friendId = f.getRequesterId().equals(currentUserId)
                ? f.getAddresseeId()
                : f.getRequesterId();
            userRepository.findById(friendId).ifPresent(friend ->
                entries.add(buildEntry(friend, false))
            );
        }

        return entries;
    }

    public HeadToHeadDto getHeadToHead(String currentUserId, String friendId) {
        User currentUser = userRepository.findById(currentUserId).orElseThrow();
        User friend = userRepository.findById(friendId).orElseThrow();
        return new HeadToHeadDto(buildH2HEntry(currentUser), buildH2HEntry(friend));
    }

    private HeadToHeadEntry buildH2HEntry(User user) {
        List<Visit> visits = visitRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        long favouriteCount = favouriteRepository.countByUserId(user.getId());
        long guinnessCount = guinnessReviewRepository.countByUserId(user.getId());
        long badgesEarned = badgeService.getBadges(user.getId()).stream().filter(b -> b.earned()).count();

        List<CoordDto> coords = visits.stream()
            .filter(v -> v.getLatitude() != null && v.getLongitude() != null)
            .map(v -> new CoordDto(v.getLatitude(), v.getLongitude()))
            .toList();

        return new HeadToHeadEntry(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            visits.size(),
            (int) favouriteCount,
            (int) guinnessCount,
            (int) badgesEarned,
            coords
        );
    }

    private LeaderboardEntry buildEntry(User user, boolean isCurrentUser) {
        List<Visit> visits = visitRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        long badgesEarned = badgeEventRepository.countByUserId(user.getId());

        List<CoordDto> coords = visits.stream()
            .filter(v -> v.getLatitude() != null && v.getLongitude() != null)
            .map(v -> new CoordDto(v.getLatitude(), v.getLongitude()))
            .toList();

        return new LeaderboardEntry(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            isCurrentUser,
            visits.size(),
            (int) badgesEarned,
            coords
        );
    }
}
