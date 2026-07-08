package com.wrg.dto;

import com.wrg.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class UserSummary {
    private Long id;
    private String fullName;
    private String email;
    private Role role;
}
