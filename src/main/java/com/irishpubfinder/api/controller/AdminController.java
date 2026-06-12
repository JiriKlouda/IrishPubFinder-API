package com.irishpubfinder.api.controller;

import com.irishpubfinder.api.dto.AdminUserDto;
import com.irishpubfinder.api.dto.UpdateRoleRequest;
import com.irishpubfinder.api.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// All endpoints under /api/admin/** are restricted to ROLE_ADMIN by SecurityConfig.
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<AdminUserDto>> listUsers() {
        return ResponseEntity.ok(adminUserService.listUsers());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserDto> updateRole(
        @AuthenticationPrincipal String actingUserId,
        @PathVariable("id") String id,
        @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(adminUserService.updateRole(actingUserId, id, request.role()));
    }
}
