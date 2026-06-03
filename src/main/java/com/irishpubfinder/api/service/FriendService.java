package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.FriendResponse;
import com.irishpubfinder.api.dto.FriendSendRequest;
import com.irishpubfinder.api.dto.PendingRequestResponse;
import com.irishpubfinder.api.exception.DuplicateFriendRequestException;
import com.irishpubfinder.api.exception.FriendshipNotFoundException;
import com.irishpubfinder.api.exception.UserNotFoundException;
import com.irishpubfinder.api.model.Friendship;
import com.irishpubfinder.api.model.FriendshipStatus;
import com.irishpubfinder.api.model.User;
import com.irishpubfinder.api.repository.FriendshipRepository;
import com.irishpubfinder.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional
    public void sendRequest(String requesterId, FriendSendRequest request) {
        User addressee = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new UserNotFoundException("No account found for that email address"));

        if (addressee.getId().equals(requesterId)) {
            throw new DuplicateFriendRequestException("You cannot add yourself as a friend");
        }

        if (friendshipRepository.existsBetweenUsers(requesterId, addressee.getId())) {
            throw new DuplicateFriendRequestException("A friend request already exists between these accounts");
        }

        friendshipRepository.save(Friendship.builder()
            .requesterId(requesterId)
            .addresseeId(addressee.getId())
            .status(FriendshipStatus.PENDING)
            .build());
    }

    public List<FriendResponse> getFriends(String userId) {
        return friendshipRepository.findAcceptedFriendships(userId).stream()
            .map(f -> {
                String otherId = f.getRequesterId().equals(userId) ? f.getAddresseeId() : f.getRequesterId();
                User other = userRepository.findById(otherId).orElseThrow();
                return new FriendResponse(
                    String.valueOf(f.getId()),
                    other.getId(),
                    other.getEmail(),
                    other.getDisplayName(),
                    f.getCreatedAt().toString()
                );
            })
            .toList();
    }

    public List<PendingRequestResponse> getPendingRequests(String userId) {
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING).stream()
            .map(f -> {
                User requester = userRepository.findById(f.getRequesterId()).orElseThrow();
                return new PendingRequestResponse(
                    String.valueOf(f.getId()),
                    f.getRequesterId(),
                    requester.getEmail(),
                    requester.getDisplayName(),
                    f.getCreatedAt().toString()
                );
            })
            .toList();
    }

    @Transactional
    public void acceptRequest(String userId, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new FriendshipNotFoundException("Friend request not found"));

        if (!friendship.getAddresseeId().equals(userId)) {
            throw new FriendshipNotFoundException("Friend request not found");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
    }

    @Transactional
    public void declineOrRemove(String userId, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found"));

        if (!friendship.getRequesterId().equals(userId) && !friendship.getAddresseeId().equals(userId)) {
            throw new FriendshipNotFoundException("Friendship not found");
        }

        friendshipRepository.delete(friendship);
    }
}
