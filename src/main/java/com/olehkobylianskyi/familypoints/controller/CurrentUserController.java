package com.olehkobylianskyi.familypoints.controller;

import com.olehkobylianskyi.familypoints.dto.CurrentUserResponse;
import com.olehkobylianskyi.familypoints.dto.OwnPasswordChangeRequest;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import com.olehkobylianskyi.familypoints.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class CurrentUserController {

    private final CurrentUserService currentUserService;
    private final UserAccountService userAccountService;

    public CurrentUserController(CurrentUserService currentUserService, UserAccountService userAccountService) {
        this.currentUserService = currentUserService;
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        return ResponseEntity.ok(CurrentUserResponse.from(currentUserService.getCurrentAccount()));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody OwnPasswordChangeRequest request) {
        userAccountService.changeOwnPassword(
                currentUserService.getCurrentAccount(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.noContent().build();
    }
}
