package com.flowops.organization;

import com.flowops.api.Envelopes;
import com.flowops.common.ratelimit.RateLimiter;
import com.flowops.security.AuthenticatedUser;
import com.flowops.security.FlowOpsPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redeeming an invitation, kept separate from {@link OrganizationController} because
 * the operative tenant is the <em>invitation's</em> org, not the caller's current
 * one — so the {@code /api/organizations/current/...} base would be misleading.
 *
 * <p>Authenticated (any role): the caller must already have a FlowOps account, and
 * the invitation's email must match theirs. Throttled per-user so a stolen session
 * cannot brute-force tokens. {@code consumes = application/json} is the same CSRF
 * defense used across the auth surface.
 */
@RestController
@RequestMapping("/api/invitations")
@Tag(name = "Organizations")
public class InvitationController {

    private static final Duration ACCEPT_WINDOW = Duration.ofMinutes(15);
    private static final int ACCEPT_LIMIT = 20;

    private final OrganizationService organizationService;
    private final RateLimiter rateLimiter;

    public InvitationController(
            OrganizationService organizationService, RateLimiter rateLimiter) {
        this.organizationService = organizationService;
        this.rateLimiter = rateLimiter;
    }

    /** Redeems a token for the caller, joining the invitation's org at its role. */
    @PostMapping(path = "/accept", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Accept an invitation and join its organization")
    public Envelopes.OrganizationContext accept(
            @Valid @RequestBody AcceptInvitationRequest request, HttpServletRequest http) {
        FlowOpsPrincipal principal = AuthenticatedUser.require();
        rateLimiter.check("invite-accept:user:" + principal.userId(), ACCEPT_LIMIT, ACCEPT_WINDOW);
        return organizationService.acceptInvitation(principal, request.token());
    }
}
