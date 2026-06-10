package com.irishpubfinder.api.service;

import com.irishpubfinder.api.dto.ContactLookupResult;
import com.irishpubfinder.api.dto.ContactsLookupRequest;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional
    public void sendRequest(String requesterId, FriendSendRequest request) {
        User addressee;
        if (request.userId() != null && !request.userId().isBlank()) {
            addressee = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException("No account found"));
        } else if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            String phone = request.phoneNumber().trim().replaceAll("[\\s\\-()]", "");
            addressee = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new UserNotFoundException("No account found for that phone number"));
        } else if (request.email() != null && !request.email().isBlank()) {
            addressee = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new UserNotFoundException("No account found for that email address"));
        } else {
            throw new UserNotFoundException("No account found");
        }

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

    public List<ContactLookupResult> findByPhoneNumbers(ContactsLookupRequest request) {
        if (request.phoneNumbers() == null || request.phoneNumbers().isEmpty()) {
            return List.of();
        }
        List<String> normalised = request.phoneNumbers().stream()
            .filter(p -> p != null && !p.isBlank())
            .map(p -> p.trim().replaceAll("[\\s\\-()]", ""))
            .distinct()
            .collect(Collectors.toList());
        return userRepository.findByPhoneNumberIn(normalised).stream()
            .map(u -> new ContactLookupResult(u.getPhoneNumber(), u.getId(), u.getDisplayName(), u.getEmail()))
            .toList();
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
