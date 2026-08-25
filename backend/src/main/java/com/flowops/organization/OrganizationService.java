package com.flowops.organization;

import com.flowops.api.AuthResponse;
import com.flowops.api.Envelopes;
import com.flowops.api.MembershipResponse;
import com.flowops.api.OrganizationMemberResponse;
import com.flowops.api.OrganizationResponse;
import com.flowops.api.SessionResponse;
import com.flowops.auth.AuthResult;
import com.flowops.auth.SessionAssembler;
import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.AuthSession;
import com.flowops.domain.Organization;
import com.flowops.domain.OrganizationMember;
import com.flowops.repository.AuthSessionRepository;
import com.flowops.repository.OrganizationMemberRepository;
import com.flowops.repository.OrganizationRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The six organization endpoints (contract §5.6–§5.11).
 *
 * <p>Every read is scoped by the authenticated principal's {@code organizationId}
 * claim (contract §2). The single method that accepts an org id from the client —
 * {@link #switchOrganization} — validates it against the caller's memberships
 * before use, and returns {@code 404} (never {@code 403}) for a non-member so a
 * caller cannot enumerate other tenants (contract §2.1).
 */
@Service
public class OrganizationService {

    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;
    private final AuthSessionRepository sessions;
    private final OrganizationCreator organizationCreator;
    private final SessionAssembler sessionAssembler;

    public OrganizationService(
            OrganizationRepository organizations,
            OrganizationMemberRepository members,
            AuthSessionRepository sessions,
            OrganizationCreator organizationCreator,
            SessionAssembler sessionAssembler) {
        this.organizations = organizations;
        this.members = members;
        this.sessions = sessions;
        this.organizationCreator = organizationCreator;
        this.sessionAssembler = sessionAssembler;
    }

    /** Every org the caller belongs to — the switcher payload (contract §5.6). */
    @Transactional(readOnly = true)
    public Envelopes.Memberships listMemberships(FlowOpsPrincipal principal) {
        List<MembershipResponse> memberships = members.findMembershipRows(principal.userId()).stream()
                .map(MembershipResponse::of)
                .toList();
        return new Envelopes.Memberships(memberships);
    }

    /** The current org with the caller's role and a live member count (contract §5.7). */
    @Transactional(readOnly = true)
    public Envelopes.OrganizationContext currentContext(FlowOpsPrincipal principal) {
        OrganizationMember membership = requireMembership(principal);
        Organization organization = requireOrganization(principal.organizationId());
        long memberCount = members.countByOrganizationId(organization.getId());
        return Envelopes.OrganizationContext.of(
                OrganizationResponse.of(organization), membership.getRole(), memberCount);
    }

    /**
     * Renames the current org (contract §5.8). Role is enforced in the controller
     * ({@code ADMIN} or {@code OWNER}); the slug is immutable and untouched.
     */
    @Transactional
    public Envelopes.OrganizationContext renameCurrent(
            FlowOpsPrincipal principal, RenameOrganizationRequest request) {
        OrganizationMember membership = requireMembership(principal);
        Organization organization = requireOrganization(principal.organizationId());
        organization.rename(request.name());
        long memberCount = members.countByOrganizationId(organization.getId());
        return Envelopes.OrganizationContext.of(
                OrganizationResponse.of(organization), membership.getRole(), memberCount);
    }

    /**
     * Creates an additional org with the caller as {@code OWNER} (contract §5.9).
     *
     * <p>Does <em>not</em> switch the caller in: the access token and session are
     * unchanged, and no cookie is set. To enter the new org, call
     * {@link #switchOrganization}.
     */
    public Envelopes.OrganizationContext createOrganization(
            FlowOpsPrincipal principal, CreateOrganizationRequest request) {
        OrganizationCreator.Created created;
        try {
            created = organizationCreator.create(principal.userId(), request.organizationName());
        } catch (DataIntegrityViolationException race) {
            // The only unique constraint reachable here is the org slug, recomputed
            // on each attempt; a single retry resolves a concurrent slug collision.
            created = organizationCreator.create(principal.userId(), request.organizationName());
        }
        return Envelopes.OrganizationContext.created(
                OrganizationResponse.of(created.organization()), created.membership().getRole());
    }

    /** Members of the current org, readable by any role (contract §5.11). */
    @Transactional(readOnly = true)
    public Envelopes.Members listMembers(FlowOpsPrincipal principal) {
        // Confirms the claim still maps to a membership before exposing the list.
        requireMembership(principal);
        List<OrganizationMemberResponse> memberList =
                members.findMemberRows(principal.organizationId()).stream()
                        .map(OrganizationMemberResponse::of)
                        .toList();
        return new Envelopes.Members(memberList);
    }

    /**
     * Switches the caller's session into another of their orgs and mints a new
     * access token carrying the new {@code orgId} and role (contract §5.10).
     *
     * <p>The refresh cookie is deliberately not rotated: the session row now holds
     * the new org, so a later refresh returns the switched tenant.
     */
    @Transactional
    public AuthResult switchOrganization(FlowOpsPrincipal principal, UUID targetOrganizationId) {
        OrganizationMember membership = members
                .findByUserIdAndOrganizationId(principal.userId(), targetOrganizationId)
                // Non-member and nonexistent org are indistinguishable by design.
                .orElseThrow(() -> new ApiException(ErrorCode.ORGANIZATION_NOT_FOUND));

        AuthSession session = sessions.findById(principal.sessionId())
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_INVALID));
        session.switchOrganization(targetOrganizationId);

        SessionResponse snapshot =
                sessionAssembler.assemble(principal.userId(), targetOrganizationId);
        AuthResponse response = sessionAssembler.withAccessToken(
                snapshot,
                principal.userId(),
                session.getId(),
                targetOrganizationId,
                membership.getRole());

        return AuthResult.tokenOnly(response);
    }

    private OrganizationMember requireMembership(FlowOpsPrincipal principal) {
        return members
                .findByUserIdAndOrganizationId(principal.userId(), principal.organizationId())
                // The token's orgId no longer maps to a membership: removed mid-token.
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN_ROLE));
    }

    private Organization requireOrganization(UUID organizationId) {
        return organizations.findById(organizationId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORGANIZATION_NOT_FOUND));
    }
}
