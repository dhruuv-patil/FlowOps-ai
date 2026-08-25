package com.flowops.auth;

import com.flowops.api.AuthResponse;
import com.flowops.api.SessionResponse;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.config.AuthProperties;
import com.flowops.domain.AuthSession;
import com.flowops.domain.Organization;
import com.flowops.domain.OrganizationMember;
import com.flowops.domain.RefreshToken;
import com.flowops.domain.Role;
import com.flowops.domain.UserAccount;
import com.flowops.repository.AuthSessionRepository;
import com.flowops.repository.OrganizationMemberRepository;
import com.flowops.repository.OrganizationRepository;
import com.flowops.repository.RefreshTokenRepository;
import com.flowops.repository.UserRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login, refresh rotation, logout and the {@code me} read
 * (contract §5.1–§5.5).
 *
 * <p>No method here logs a password, a password hash, a refresh token or an access
 * token — at any level (contract §9). The only credential-adjacent value that ever
 * reaches a log line is a user id.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * BCrypt silently ignores input past 72 bytes, so without this cap two
     * different long passwords would authenticate interchangeably.
     */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final AccountCreator accountCreator;
    private final SessionAssembler sessionAssembler;
    private final PasswordEncoder passwordEncoder;
    private final Duration refreshTokenTtl;

    /**
     * A throwaway hash compared against when the email is unknown, so a login
     * attempt costs one BCrypt verification either way. Without it, response time
     * alone reveals which addresses are registered.
     */
    private final String timingEqualizerHash;

    public AuthService(
            UserRepository users,
            OrganizationRepository organizations,
            OrganizationMemberRepository members,
            AuthSessionRepository sessions,
            RefreshTokenRepository refreshTokens,
            AccountCreator accountCreator,
            SessionAssembler sessionAssembler,
            PasswordEncoder passwordEncoder,
            AuthProperties properties) {
        this.users = users;
        this.organizations = organizations;
        this.members = members;
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.accountCreator = accountCreator;
        this.sessionAssembler = sessionAssembler;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenTtl = properties.refreshTokenTtl();
        this.timingEqualizerHash = passwordEncoder.encode(OpaqueTokens.mint());
    }

    /**
     * Creates user + organization + {@code OWNER} membership atomically, then opens
     * a session (contract §5.1).
     *
     * <p>Deliberately <em>not</em> {@code @Transactional}: {@link AccountCreator}
     * owns its own transaction so that a unique-constraint violation rolls back
     * only the insert, leaving this method able to query the database to decide
     * which constraint failed. A shared transaction would be marked rollback-only
     * and every follow-up query would fail.
     */
    public AuthResult register(RegisterRequest request, String userAgent, String ip) {
        assertPasswordByteLength(request.password());

        String passwordHash = passwordEncoder.encode(request.password());
        AccountCreator.Created created;
        try {
            created = accountCreator.create(
                    request.email(), passwordHash, request.fullName(), request.organizationName());
        } catch (DataIntegrityViolationException first) {
            created = retryOrTranslate(request, passwordHash, first);
        }

        return startSession(
                created.user(),
                created.organization(),
                created.membership().getRole(),
                userAgent,
                ip);
    }

    /**
     * Decides what a constraint violation during registration actually was. The
     * email index is checked first because that is the case the client can act on;
     * anything else is two organizations racing for one slug, which a fresh attempt
     * resolves because the slug is recomputed.
     */
    private AccountCreator.Created retryOrTranslate(
            RegisterRequest request, String passwordHash, DataIntegrityViolationException cause) {

        if (users.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        try {
            return accountCreator.create(
                    request.email(), passwordHash, request.fullName(), request.organizationName());
        } catch (DataIntegrityViolationException retry) {
            if (users.findByEmailIgnoreCase(request.email()).isPresent()) {
                throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
            }
            log.error("Registration failed twice on a constraint violation.", retry);
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Opens a <em>new</em> session — a new {@code sid} — leaving sessions on other
     * devices untouched (contract §5.2).
     */
    public AuthResult login(LoginRequest request, String userAgent, String ip) {
        UserAccount user = users.findByEmailIgnoreCase(request.email()).orElse(null);

        if (user == null) {
            // Burn an equivalent BCrypt verification so an unknown address is not
            // distinguishable from a wrong password by response time.
            passwordEncoder.matches(request.password(), timingEqualizerHash);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        OrganizationMember membership = members
                .findFirstByUserIdOrderByJoinedAtAscOrganizationIdAsc(user.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.NO_ORGANIZATION_CONTEXT));

        Organization organization = requireOrganization(membership.getOrganizationId());
        return startSession(user, organization, membership.getRole(), userAgent, ip);
    }

    /**
     * Rotates the refresh token and returns a full {@link AuthResponse}
     * (contract §5.3, §1.3).
     *
     * <p>Returning the whole session rather than just a token makes app boot one
     * round trip instead of {@code refresh} + {@code me}, and re-reads user, org and
     * memberships from the database on every reload so nothing goes stale.
     */
    @Transactional
    public AuthResult refresh(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        RefreshToken stored = refreshTokens
                .findByTokenHash(OpaqueTokens.hash(presentedToken))
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));

        AuthSession session = sessions.findById(stored.getSessionId())
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (stored.isSpent()) {
            revokeEverythingAfterReuse(session.getUserId());
        }
        if (stored.isExpired(Instant.now()) || session.isRevoked()) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        stored.consume();
        OrganizationMember membership = resolveSessionMembership(session);
        session.touch();

        UserAccount user = users.findById(session.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));
        Organization organization = requireOrganization(membership.getOrganizationId());

        String rotated = issueRefreshToken(session.getId());
        SessionResponse snapshot =
                sessionAssembler.assemble(user, organization, membership.getRole());
        AuthResponse response = sessionAssembler.withAccessToken(
                snapshot, user.getId(), session.getId(), organization.getId(), membership.getRole());

        return AuthResult.rotating(response, rotated);
    }

    /**
     * A token that exists but is already used or revoked means the cookie leaked and
     * two parties now hold it. There is no way to tell the thief from the victim, so
     * every session and every refresh token for the user is destroyed and they must
     * sign in again everywhere.
     *
     * <p>Because this is destructive, the frontend must single-flight refresh
     * (contract §6.2) — otherwise two parallel 401s from one tab would look exactly
     * like theft.
     */
    private void revokeEverythingAfterReuse(UUID userId) {
        Instant now = Instant.now();
        refreshTokens.revokeAllForUser(userId, now);
        sessions.revokeAllForUser(userId, now);
        log.warn("Refresh token reuse detected for user {}; all sessions revoked.", userId);
        throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    /**
     * Re-verifies that the session's organization is still one the user belongs to
     * (they may have been removed since the last refresh) and falls back to the
     * earliest-joined membership otherwise (contract §2.2).
     */
    private OrganizationMember resolveSessionMembership(AuthSession session) {
        return members
                .findByUserIdAndOrganizationId(session.getUserId(), session.getOrganizationId())
                .orElseGet(() -> {
                    OrganizationMember fallback = members
                            .findFirstByUserIdOrderByJoinedAtAscOrganizationIdAsc(session.getUserId())
                            .orElseThrow(() -> new ApiException(ErrorCode.NO_ORGANIZATION_CONTEXT));
                    session.switchOrganization(fallback.getOrganizationId());
                    return fallback;
                });
    }

    /**
     * Revokes the session and every refresh token in it (contract §5.4).
     *
     * <p><strong>Idempotent and never 401.</strong> A missing cookie, an expired
     * token, garbage, or an already-dead session all return normally — a user
     * clicking "log out" on a stale tab has to end up logged out.
     *
     * @param sessionIdFromToken the {@code sid} of a valid bearer token, or null
     * @param presentedRefreshToken the cookie value, used when there is no bearer
     */
    @Transactional
    public void logout(UUID sessionIdFromToken, String presentedRefreshToken) {
        UUID sessionId = sessionIdFromToken;

        if (sessionId == null && presentedRefreshToken != null && !presentedRefreshToken.isBlank()) {
            sessionId = refreshTokens
                    .findByTokenHash(OpaqueTokens.hash(presentedRefreshToken))
                    .map(RefreshToken::getSessionId)
                    .orElse(null);
        }

        if (sessionId == null) {
            return;
        }

        // Revoke the session first: the bulk token update flushes pending changes,
        // so ordering it this way needs no explicit flush.
        sessions.findById(sessionId).ifPresent(AuthSession::revoke);
        refreshTokens.revokeAllForSession(sessionId, Instant.now());
    }

    /** {@code GET /api/auth/me} — identity and tenant context read fresh (contract §5.5). */
    @Transactional(readOnly = true)
    public SessionResponse currentSession(FlowOpsPrincipal principal) {
        return sessionAssembler.assemble(principal.userId(), principal.organizationId());
    }

    /**
     * Opens a session and mints its first refresh token.
     *
     * <p>Not transactional, and that is a deliberate, bounded tradeoff: if the token
     * insert fails after the session insert commits, the leftover session row has no
     * usable token and is inert. Wrapping the two would mean {@code register}'s
     * orchestration had to share a transaction with {@link AccountCreator}, which is
     * exactly what the constraint-violation handling above cannot tolerate.
     */
    private AuthResult startSession(
            UserAccount user, Organization organization, Role role, String userAgent, String ip) {

        AuthSession session = sessions.save(
                AuthSession.create(user.getId(), organization.getId(), userAgent, ip));

        String refreshToken = issueRefreshToken(session.getId());
        SessionResponse snapshot = sessionAssembler.assemble(user, organization, role);
        AuthResponse response = sessionAssembler.withAccessToken(
                snapshot, user.getId(), session.getId(), organization.getId(), role);

        return AuthResult.rotating(response, refreshToken);
    }

    /** Persists only the hash; the plaintext is returned for the cookie and then dropped. */
    private String issueRefreshToken(UUID sessionId) {
        String token = OpaqueTokens.mint();
        refreshTokens.save(RefreshToken.create(
                sessionId, OpaqueTokens.hash(token), Instant.now().plus(refreshTokenTtl)));
        return token;
    }

    private Organization requireOrganization(UUID organizationId) {
        return organizations.findById(organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORGANIZATION_NOT_FOUND));
    }

    /**
     * Enforces the UTF-8 byte cap that {@code @Pattern} cannot express: a multi-byte
     * password can pass an 8–72 <em>character</em> check and still exceed 72 bytes,
     * at which point BCrypt would silently truncate it.
     */
    private void assertPasswordByteLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw ApiException.validation(
                    "password",
                    "Password must be 8-72 characters and include at least one letter"
                            + " and one number.");
        }
    }




}
