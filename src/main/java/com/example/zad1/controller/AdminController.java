package com.example.zad1.controller;

import com.example.zad1.model.User;
import com.example.zad1.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserAdminService userAdminService;

    // В ответе нет пароля (даже хэша)
    public record UserResponse(Long id, String name, String email, User.Role role, User.Status status) {
    }

    public record StatusRequest(User.Status status) {
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userAdminService.getAll().stream().map(this::toResponse).toList();
    }

    @PatchMapping("/{id}/status")
    public UserResponse changeStatus(@PathVariable Long id, @RequestBody StatusRequest request,
                                     Principal principal) {
        return toResponse(userAdminService.changeStatus(id, request.status(), principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        userAdminService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getStatus());
    }
}