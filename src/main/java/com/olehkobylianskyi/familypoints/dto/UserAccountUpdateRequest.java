package com.olehkobylianskyi.familypoints.dto;

import jakarta.validation.constraints.NotBlank;

public class UserAccountUpdateRequest {
    @NotBlank
    private String username;
    private boolean enabled;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
