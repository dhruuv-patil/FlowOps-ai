package com.flowops.auth;

import com.flowops.api.AuthResponse;
import com.flowops.api.MembershipResponse;
import com.flowops.api.OrganizationResponse;
import com.flowops.api.SessionResponse;
import com.flowops.api.UserResponse;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.Organization;
import com.flowops.domain.OrganizationMember;
import com.flowops.domain.Role;
import com.flowops.domain.UserAccount;
import com.flowops.repository.OrganizationMemberRepository;
import com.flowops.repository.OrganizationRepository;
import com.flowops.repository.UserRepository;
import com.flowops.security.JwtService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the {@link SessionResponse} / {@link AuthResponse} payloads shared by
 * register, login, refresh, switch-org and {@code me} (contract §4.5).
 *
 * <p>Centralized so there is exactly one place that decides what "the current
 * session" looks like — five endpoints returning the same shape assembled five
 * different ways is how {@code currentRole} and {@code memberships} drift apart.
 */
@Component
public class SessionAssembler {

    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;
    private final JwtService jwtService;

    public SessionAssembler(
            UserRepository users,
            OrganizationRepository organizations,
            OrganizationMemberRepository members,
            JwtService jwtService) {
        this.users = users;
        this.organizations = organizations;
        this.members = members;
        this.jwtService = jwtService;
    }

    /**
     * Reads identity and tenant context fresh from the database for an already
     * verified {@code (userId, organizationId)} pair.
     *
     * @throws ApiException {@code 401 TOKEN_INVALID} if the user no longer exists,
     *     {@code 403 FORBIDDEN_ROLE} if the token's {@code orgId} is no longer one
     *     of their memberships (they were removed mid-token)
     */
    @Transactional(readOnly = true)
    public SessionResponse assemble(UUID userId, UUID organizationId) {
        UserAccount user = users.findById(userId)
                // The token verified, so this means the row was deleted after issue.
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));

        OrganizationMember membership = members
                .findByUserIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN_ROLE));

        Organization organization = organizations.findById(organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORGANIZATION_NOT_FOUND));

        return assemble(user, organization, membership.getRole());
    }

    /** Assembles from entities already loaded in the caller's transaction. */
    @Transactional(readOnly = true)
    public SessionResponse assemble(UserAccount user, Organization organization, Role role) {
        List<MembershipResponse> memberships = members.findMembershipRows(user.getId()).stream()
                .map(MembershipResponse::of)
                .toList();

        return new SessionResponse(
                UserResponse.of(user),
                OrganizationResponse.of(organization),
                role,
                memberships);
    }

    /** Mints an access token for the session and wraps it in an {@link AuthResponse}. */
    public AuthResponse withAccessToken(
            SessionResponse session, UUID userId, UUID sessionId, UUID organizationId, Role role) {

        String accessToken = jwtService.issueAccessToken(
                userId, sessionId, organizationId, role, session.user().email());

        return AuthResponse.of(accessToken, jwtService.accessTokenTtlSeconds(), session);
    }
}
