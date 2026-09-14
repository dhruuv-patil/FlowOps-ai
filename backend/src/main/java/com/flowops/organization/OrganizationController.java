package com.flowops.organization;

import com.flowops.api.AuthResponse;
import com.flowops.api.Envelopes;
import com.flowops.api.InvitationSecretResponse;
import com.flowops.auth.AuthResult;
import com.flowops.common.ratelimit.RateLimiter;
import com.flowops.domain.Role;
import com.flowops.security.AuthenticatedUser;
import com.flowops.security.FlowOpsPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The six organization endpoints (contract §5.6–§5.11).
 *
 * <p>The tenant is read from the principal on every route except {@code switch},
 * which takes a selector validated in the service. {@code /current} is mapped
 * before the {@code {organizationId}} pattern so {@code current} is never parsed
 * as a UUID.
 */
@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organizations")
public class OrganizationController {

    private static final Duration CREATE_WINDOW = Duration.ofMinutes(60);
    private static final int CREATE_LIMIT = 20;

    private final OrganizationService organizationService;
    private final RateLimiter rateLimiter;

    public OrganizationController(
            OrganizationService organizationService, RateLimiter rateLimiter) {
        this.organizationService = organizationService;
        this.rateLimiter = rateLimiter;
    }

    /** Every org the caller belongs to (contract §5.6). */
    @GetMapping
    @Operation(summary = "List the authenticated user's organizations")
    public Envelopes.Memberships list() {
        return organizationService.listMemberships(AuthenticatedUser.require());
    }

    /** The current org, the caller's role, and a live member count (contract §5.7). */
    @GetMapping("/current")
    @Operation(summary = "The current organization derived from the token")
    public Envelopes.OrganizationContext current() {
        return organizationService.currentContext(AuthenticatedUser.require());
    }

    /** Renames the current org; requires {@code ADMIN} or {@code OWNER} (contract §5.8). */
    @PatchMapping(path = "/current", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rename the current organization (ADMIN or OWNER)")
    public Envelopes.OrganizationContext rename(
            @Valid @RequestBody RenameOrganizationRequest request) {
        FlowOpsPrincipal principal = AuthenticatedUser.requireRole(Role.ADMIN);
        return organizationService.renameCurrent(principal, request);
    }

    /** Members of the current org, readable by any role (contract §5.11). */
    @GetMapping("/current/members")
    @Operation(summary = "List members of the current organization")
    public Envelopes.Members members() {
        return organizationService.listMembers(AuthenticatedUser.require());
    }

    /** Changes a member's role; requires {@code ADMIN} or {@code OWNER}. */
    @PatchMapping(path = "/current/members/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Change a member's role (ADMIN or OWNER)")
    public Envelopes.Members changeMemberRole(
            @PathVariable UUID userId, @Valid @RequestBody ChangeMemberRoleRequest request) {
        FlowOpsPrincipal principal = AuthenticatedUser.requireRole(Role.ADMIN);
        return organizationService.changeMemberRole(principal, userId, request.role());
    }

    /** Removes a member from the current org; requires {@code ADMIN} or {@code OWNER}. */
    @DeleteMapping("/current/members/{userId}")
    @Operation(summary = "Remove a member from the current organization (ADMIN or OWNER)")
    public Envelopes.Members removeMember(@PathVariable UUID userId) {
        FlowOpsPrincipal principal = AuthenticatedUser.requireRole(Role.ADMIN);
        return organizationService.removeMember(principal, userId);
    }

    /** Pending/accepted/revoked invitations; requires {@code ADMIN} or {@code OWNER}. */
    @GetMapping("/current/invitations")
    @Operation(summary = "List invitations for the current organization (ADMIN or OWNER)")
    public Envelopes.Invitations invitations() {
        return organizationService.listInvitations(AuthenticatedUser.requireRole(Role.ADMIN));
    }

    /**
     * Creates (or re-mints) a tokened invitation; requires {@code ADMIN} or
     * {@code OWNER}. Returns the raw token exactly once — it is never recoverable
     * afterwards.
     */
    @PostMapping(path = "/current/invitations", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite a user to the current organization (ADMIN or OWNER)")
    public InvitationSecretResponse invite(@Valid @RequestBody InviteMemberRequest request) {
        FlowOpsPrincipal principal = AuthenticatedUser.requireRole(Role.ADMIN);
        return organizationService.invite(principal, request);
    }

    /** Revokes a pending invitation; requires {@code ADMIN} or {@code OWNER}. */
    @DeleteMapping("/current/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke an invitation (ADMIN or OWNER)")
    public void revokeInvitation(@PathVariable UUID invitationId) {
        FlowOpsPrincipal principal = AuthenticatedUser.requireRole(Role.ADMIN);
        organizationService.revokeInvitation(principal, invitationId);
    }

    /** Creates an additional org without switching into it (contract §5.9). */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an additional organization owned by the caller")
    public ResponseEntity<Envelopes.OrganizationContext> create(
            @Valid @RequestBody CreateOrganizationRequest request, HttpServletRequest http) {
        FlowOpsPrincipal principal = AuthenticatedUser.require();
        rateLimiter.check("create-org:user:" + principal.userId(), CREATE_LIMIT, CREATE_WINDOW);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.createOrganization(principal, request));
    }

    /**
     * Switches the session into another of the caller's orgs (contract §5.10).
     *
     * <p>No body; nothing is parsed, so no {@code consumes} constraint. Returns the
     * whole {@link AuthResponse} with a new access token and <strong>no</strong>
     * {@code Set-Cookie} — the refresh cookie is not rotated.
     */
    @PostMapping("/{organizationId}/switch")
    @Operation(summary = "Switch the active organization and mint a new access token")
    public AuthResponse switchOrganization(@PathVariable UUID organizationId) {
        FlowOpsPrincipal principal = AuthenticatedUser.require();
        AuthResult result = organizationService.switchOrganization(principal, organizationId);
        return result.response();
    }
}
