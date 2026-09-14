package com.flowops.auth;

import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.common.ratelimit.RateLimiter;
import com.flowops.config.AuthProperties;
import com.flowops.domain.PasswordResetToken;
import com.flowops.domain.UserAccount;
import com.flowops.repository.AuthSessionRepository;
import com.flowops.repository.PasswordResetTokenRepository;
import com.flowops.repository.RefreshTokenRepository;
import com.flowops.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Password reset flow: forgot-password → reset-password (contract §5.x).
 *
 * <p>Two invariants:
 * <ol>
 *   <li>The reset token is generated as 256 bits of {@link SecureRandom} output,
 *       hashed with SHA-256 for storage, and the plaintext is only ever
 *       communicated via the email abstraction (never logged, never returned).
 *   <li>The forgot-password endpoint always returns a generic success response
 *       (never reveals whether the email exists in the system).
 * </ol>
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final RefreshTokenRepository refreshTokens;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final Duration resetTokenTtl;
    private final RateLimiter rateLimiter;
    private final PasswordResetMailer resetMailer;

    /**
     * A throwaway hash compared against when the email is unknown, so a request
     * costs one BCrypt verification either way. Without it, response time alone
     * reveals which addresses are registered.
     */
    private final String timingEqualizerHash;

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository resetTokens,
            RefreshTokenRepository refreshTokens,
            AuthSessionRepository sessions,
            PasswordEncoder passwordEncoder,
            AuthProperties properties,
            RateLimiter rateLimiter,
            PasswordResetMailer resetMailer) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.resetTokenTtl = properties.resetTokenTtl();
        this.rateLimiter = rateLimiter;
        this.resetMailer = resetMailer;
        this.timingEqualizerHash = passwordEncoder.encode(OpaqueTokens.mint());
    }

    /**
     * Initiates a password reset for the given email (contract §5.x).
     *
     * <p>Always returns normally with a generic message — never reveals whether
     * the email is registered. If the email exists, a single-use reset token is
     * generated, hashed, and stored; the plaintext is handed straight to the
     * {@link PasswordResetMailer} (it travels only inside the reset URL) and then
     * dropped.
     *
     * <p>Rate-limited per IP + normalized email.
     */
    @Transactional 
    public void forgotPassword(ForgotPasswordRequest request, String ip) {
        rateLimiter.check(
                "forgot-password:" + ip + ':' + request.email(),
                5,
                Duration.ofMinutes(60));

        UserAccount user = users.findByEmailIgnoreCase(request.email()).orElse(null);

        if (user == null) {
            // Burn an equivalent BCrypt verification so an unknown address is not
            // distinguishable from a known one by response time.
            passwordEncoder.matches(OpaqueTokens.mint(), timingEqualizerHash);
            return;
        }

        // Revoke any existing unused tokens for this user.
        resetTokens.revokeAllUnusedForUser(user.getId(), Instant.now());

        // Generate and store the new token.
        String token = OpaqueTokens.mint();
        String tokenHash = OpaqueTokens.hash(token);
        Instant expiresAt = Instant.now().plus(resetTokenTtl);
        resetTokens.save(PasswordResetToken.create(user.getId(), tokenHash, expiresAt));

        // Deliver the reset link — the plaintext token rides only the email (as the
        // query parameter of the reset URL) and is never logged or returned. The
        // mailer is a safe no-op when SMTP is unconfigured and never throws, so this
        // cannot change the generic 204 from the controller.
        resetMailer.sendResetEmail(user.getEmail(), token);

        log.info("Password reset token generated for user {}", user.getId());
    }

    /**
     * Completes a password reset using a token (contract §5.x).
     *
     * <p>Validates the token (exists, not used, not expired), hashes the new
     * password, updates the user, marks the token used, and revokes all refresh
     * sessions for the user so the attacker loses access immediately.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();
        String tokenHash = OpaqueTokens.hash(request.token());

        PasswordResetToken stored = resetTokens
                .findValidByTokenHash(tokenHash, now)
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));

        // The token exists and is valid — proceed.
        UserAccount user = users.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));

        // Enforce the 72-byte BCrypt cap.
        assertPasswordByteLength(request.newPassword());

        // Update the password.
        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        user.changePassword(newPasswordHash);

        // Mark the token used.
        stored.consume();

        // A compromised password must result in immediate sign-out everywhere:
        // revoke the session and every refresh token for the user. The access
        // token stays valid until its TTL, but without a live refresh token the
        // session dies at the next 401 / refresh attempt.
        refreshTokens.revokeAllForUser(user.getId(), now);
        sessions.revokeAllForUser(user.getId(), now);

        log.info("Password reset completed for user {}; all sessions revoked", user.getId());
    }

    /**
     * Enforces the UTF-8 byte cap that {@code @Pattern} cannot express: a multi-byte
     * password can pass an 8–72 <em>character</em> check and still exceed 72 bytes,
     * at which point BCrypt would silently truncate it.
     */
    private void assertPasswordByteLength(String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw ApiException.validation(
                    "newPassword",
                    "Password must be 8-72 characters and include at least one letter"
                            + " and one number.");
        }
    }
}