package com.wrg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ProjectRequest {
    @NotBlank
    private String name;

    private String description;

    /**
     * IDs of team members assigned to this project. Null or empty means the
     * project is open to every team member (the default, backward-compatible
     * behavior). Managers are always able to see and manage every project
     * regardless of this list.
     */
    private List<Long> memberIds;
}
