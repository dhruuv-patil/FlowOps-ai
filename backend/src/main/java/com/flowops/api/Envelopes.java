package com.flowops.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flowops.domain.Role;
import java.util.List;

/**
 * Object envelopes for the collection endpoints (contract §5.6, §5.7, §5.11).
 *
 * <p>Every collection is wrapped in an object with a named key rather than
 * returned as a bare top-level array: an array is awkward to extend (pagination
 * arrives in M5 for members) and is historically a JSON-hijacking footgun.
 */
public final class Envelopes {

    private Envelopes() {
    }

    /** {@code GET /api/organizations} — the org switcher payload. */
    public record Memberships(List<MembershipResponse> memberships) {
    }

    /** {@code GET /api/organizations/current/members}. */
    public record Members(List<OrganizationMemberResponse> members) {
    }

    /**
     * {@code GET}/{@code PATCH /api/organizations/current} and
     * {@code POST /api/organizations}.
     *
     * <p>{@code memberCount} is a live {@code COUNT(*)} on the current-org reads
     * and is omitted entirely on create — §5.9 returns only the org and the
     * caller's role, so the field is suppressed rather than sent as null.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrganizationContext(
            OrganizationResponse organization, Role role, Long memberCount) {

        public static OrganizationContext of(
                OrganizationResponse organization, Role role, long memberCount) {
            return new OrganizationContext(organization, role, memberCount);
        }

        public static OrganizationContext created(OrganizationResponse organization, Role role) {
            return new OrganizationContext(organization, role, null);
        }
    }
}
