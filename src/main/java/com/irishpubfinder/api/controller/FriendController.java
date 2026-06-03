package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.FriendResponse;
import com.irishpubfinder.api.dto.FriendSendRequest;
import com.irishpubfinder.api.dto.PendingRequestResponse;
import com.irishpubfinder.api.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService service;

    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(service.getFriends(userId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingRequestResponse>> getPendingRequests(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(service.getPendingRequests(userId));
    }

    @PostMapping("/request")
    public ResponseEntity<Void> sendRequest(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody FriendSendRequest request
    ) {
        service.sendRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<Void> acceptRequest(
        @AuthenticationPrincipal String userId,
        @PathVariable Long id
    ) {
        service.acceptRequest(userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> declineOrRemove(
        @AuthenticationPrincipal String userId,
        @PathVariable Long id
    ) {
        service.declineOrRemove(userId, id);
        return ResponseEntity.noContent().build();
    }
}
