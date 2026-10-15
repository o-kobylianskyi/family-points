package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.AccountWorkspaceMembership;
import com.olehkobylianskyi.familypoints.entity.UserAccount;
import com.olehkobylianskyi.familypoints.repository.AccountWorkspaceMembershipRepository;
import com.olehkobylianskyi.familypoints.repository.UserAccountRepository;
import com.olehkobylianskyi.familypoints.repository.WorkspaceMemberRepository;
import com.olehkobylianskyi.familypoints.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.mail.MessagingException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/** One-use email invitations bind a NEW login to a pre-existing member (never create a member). */
@Service
public class MemberEmailInvitationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate db;
    private final WorkspaceMemberRepository members;
    private final UserAccountRepository accounts;
    private final AccountWorkspaceMembershipRepository memberships;
    private final CurrentUserService current;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mail;
    private final String from;
    private final String webUrl;
    private final boolean enabled;

    public MemberEmailInvitationService(JdbcTemplate db, WorkspaceMemberRepository members,
            UserAccountRepository accounts, AccountWorkspaceMembershipRepository memberships,
            CurrentUserService current, PasswordEncoder passwordEncoder, JavaMailSender mail,
            @Value("${familypoints.mail.from:}") String from,
            @Value("${familypoints.web-url:http://localhost:5173}") String webUrl,
            @Value("${familypoints.member-email-invitations.enabled:false}") boolean enabled) {
        this.db = db;
        this.members = members;
        this.accounts = accounts;
        this.memberships = memberships;
        this.current = current;
        this.passwordEncoder = passwordEncoder;
        this.mail = mail;
        this.from = from;
        this.webUrl = webUrl;
        this.enabled = enabled;
    }

    private void requireEnabled() {
        if (!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Member email invitations are disabled");
    }

    private void requireMailConfig() {
        if (from.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SMTP sender not configured");
        try {
            var url = java.net.URI.create(webUrl);
            boolean local = "localhost".equalsIgnoreCase(url.getHost()) || "127.0.0.1".equals(url.getHost());
            if (url.getHost() == null || !("https".equalsIgnoreCase(url.getScheme())
                    || (local && "http".equalsIgnoreCase(url.getScheme()))))
                throw new IllegalArgumentException("Invalid link origin");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Invalid public web origin");
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional
    public void issue(Long workspaceId, Long memberId, String email) {
        requireEnabled();
        requireMailConfig();
        if (email == null || email.isBlank() || email.length() > 254)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email");
        var member = members.findByIdAndWorkspaceIdAndActiveTrue(memberId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member unavailable"));
        if (accounts.existsByWorkspaceMemberId(memberId) ||
                !db.queryForList("SELECT id FROM account_workspace_memberships WHERE family_member_id=? LIMIT 1",
                        Long.class, memberId).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member already has an account");
        Integer recent = db.queryForObject("""
            SELECT count(*) FROM member_email_invitations
            WHERE member_id=? AND created_at > CURRENT_TIMESTAMP - interval '2 minutes'
            """, Integer.class, memberId);
        if (recent != null && recent > 0)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again after two minutes");
        String secret = randomToken();
        OffsetDateTime expires = OffsetDateTime.now().plusHours(24);
        db.update("UPDATE member_email_invitations SET revoked_at=CURRENT_TIMESTAMP WHERE member_id=? AND consumed_at IS NULL AND revoked_at IS NULL",
                memberId);
        db.update("""
            INSERT INTO member_email_invitations(workspace_id,member_id,invited_email,issued_by_account_id,token_hash,expires_at)
            VALUES(?,?,?,?,?,?)
            """, workspaceId, memberId, email.trim(), current.getCurrentAccount().getId(), hash(secret), expires);
        String link = webUrl.replaceAll("/+$", "") + "/accept-member-invite?token=" + secret;
        String memberName = member.getName();
        String plain = """
                Вас запрошують до FamilyPoints!

                Вітаємо!

                Для учасника «%s» створено профіль у FamilyPoints — застосунку для
                сімейних завдань, балів і винагород.

                Щоб налаштувати особистий вхід, перейдіть за посиланням і створіть
                логін та пароль:

                %s

                Посилання одноразове та дійсне протягом 24 годин.
                Якщо ви не очікували цього запрошення, просто проігноруйте лист.

                Команда FamilyPoints
                """.formatted(memberName, link);
        String safeName = escapeHtml(memberName);
        String safeLink = escapeHtml(link);
        String html = """
                <!doctype html>
                <html lang="uk">
                  <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                  <body style="margin:0;padding:32px 16px;background:#f3f5f9;font-family:Arial,Helvetica,sans-serif;color:#202938">
                    <div style="max-width:560px;margin:0 auto;background:white;border:1px solid #e4e8f0;border-radius:14px;overflow:hidden">
                      <div style="padding:24px 32px;background:#273f89;color:white;font-size:23px;font-weight:bold">FamilyPoints</div>
                      <div style="padding:32px">
                        <h1 style="font-size:24px;line-height:1.3;margin:0 0 18px">Вас запрошують до FamilyPoints!</h1>
                        <p style="font-size:16px;line-height:1.6">Вітаємо!</p>
                        <p style="font-size:16px;line-height:1.6">Для учасника <strong>«%s»</strong> створено профіль у FamilyPoints — застосунку для сімейних завдань, балів і винагород.</p>
                        <p style="font-size:16px;line-height:1.6">Щоб отримати особистий доступ, натисніть кнопку нижче та створіть логін і пароль.</p>
                        <p style="margin:30px 0"><a href="%s" style="display:inline-block;padding:15px 24px;background:#3659c9;color:#ffffff;text-decoration:none;border-radius:8px;font-size:16px;font-weight:bold">Прийняти запрошення</a></p>
                        <p style="font-size:14px;line-height:1.6;color:#657087">Посилання діє 24 години та може бути використане лише один раз.</p>
                        <p style="font-size:14px;line-height:1.6;color:#657087">Якщо кнопка не працює, скопіюйте посилання у браузер:<br><a href="%s" style="color:#3659c9;overflow-wrap:anywhere;word-break:break-all">%s</a></p>
                      </div>
                      <div style="padding:20px 32px;border-top:1px solid #e4e8f0;background:#fafbfe;font-size:13px;line-height:1.6;color:#657087">Якщо ви не очікували запрошення, просто проігноруйте цей лист.<br>Команда FamilyPoints</div>
                    </div>
                  </body>
                </html>
                """.formatted(safeName, safeLink, safeLink, safeLink);
        try {
            var message = mail.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, "FamilyPoints");
            helper.setTo(email.trim());
            helper.setSubject("Запрошення до FamilyPoints — " + memberName);
            helper.setText(plain, html);
            mail.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException("Could not prepare member invitation email", ex);
        }
    }

    /** Creates a new global account with verified email ownership proven by possession of the mail token. */
    @Transactional
    public void accept(String secret, String username, String password) {
        requireEnabled();
        if (secret == null || !secret.matches("[A-Za-z0-9_-]{43}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invitation");
        if (username == null || !username.matches("[a-zA-Z0-9._-]{3,64}") ||
                password == null || password.length() < 8 || password.length() > 128)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials");

        record Invite(Long id, Long workspaceId, Long memberId, String email) {}
        List<Invite> found = db.query("""
            SELECT id, workspace_id, member_id, invited_email FROM member_email_invitations
            WHERE token_hash=? AND consumed_at IS NULL AND revoked_at IS NULL
              AND expires_at > CURRENT_TIMESTAMP FOR UPDATE
            """, (rs, i) -> new Invite(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getString(4)), hash(secret));
        if (found.size() != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation expired or already used");
        Invite invite = found.get(0);
        var member = members.findByIdAndWorkspaceIdAndActiveTrue(invite.memberId(), invite.workspaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member unavailable"));
        if (accounts.existsByWorkspaceMemberId(member.getId()) ||
                !db.queryForList("SELECT id FROM account_workspace_memberships WHERE family_member_id=? LIMIT 1",
                        Long.class, member.getId()).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Member already has an account");
        if (accounts.existsByUsername(username))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");

        UserAccount account = new UserAccount(username, passwordEncoder.encode(password), member);
        account.setEmail(invite.email());
        account.setEmailVerifiedAt(OffsetDateTime.now());
        account.setEnabled(true);
        UserAccount saved = accounts.saveAndFlush(account);
        memberships.save(new AccountWorkspaceMembership(saved, member));
        db.update("UPDATE member_email_invitations SET consumed_at=CURRENT_TIMESTAMP WHERE id=?", invite.id());
    }
}
