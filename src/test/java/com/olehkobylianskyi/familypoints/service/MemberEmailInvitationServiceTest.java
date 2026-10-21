package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceMemberRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberEmailInvitationServiceTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final AccountWorkspaceMembershipRepository memberships =
            mock(AccountWorkspaceMembershipRepository.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private final GoogleTokenVerifier google = mock(GoogleTokenVerifier.class);
    private final MemberEmailInvitationService service = new MemberEmailInvitationService(
            db, members, accounts, memberships, current, passwords, mail, google,
            "no-reply@example.org", "http://localhost:5173", true);

    @Test
    void rejectMalformedLinkBeforeReadingDatabase() {
        assertEquals("INVALID", service.invitationStatus("short"));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.accept("short", "username", "good-password")).getStatusCode());
        verifyNoInteractions(db, accounts, members, memberships);
    }

    @Test
    void googleRequiresVerifiedEmailBeforeUsingInvitation() {
        Jwt token = mock(Jwt.class);
        when(google.verifiedToken("google-id-token")).thenReturn(token);
        when(token.getClaimAsString("email")).thenReturn("child@example.org");
        when(token.getClaim("email_verified")).thenReturn(false);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.acceptWithGoogle("valid-does-not-matter", "google-id-token"));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(db, accounts, members, memberships);
    }

    @Test
    void googleTokenVerificationComesBeforeInvitationAccess() {
        when(google.verifiedToken("bad-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class,
                () -> service.acceptWithGoogle("opaque-link", "bad-token")).getStatusCode());
        verifyNoInteractions(db, accounts, members, memberships);
    }
}
