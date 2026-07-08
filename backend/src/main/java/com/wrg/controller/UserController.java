package com.wrg.controller;

import com.wrg.dto.UserSummary;
import com.wrg.model.Role;
import com.wrg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Manager-only: list team members, e.g. to populate filter dropdowns. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/team-members")
    public ResponseEntity<List<UserSummary>> getTeamMembers() {
        List<UserSummary> members = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TEAM_MEMBER)
                .map(u -> UserSummary.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .build())
                .toList();
        return ResponseEntity.ok(members);
    }
}
