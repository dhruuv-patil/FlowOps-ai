package com.flowops.api;

import com.flowops.domain.Role;
import java.util.List;

/**
 * {@link SessionResponse} plus the three token fields (contract §4.5). Returned
 * by register, login, refresh and switch-org — one shape, so the frontend has
 * exactly one "hydrate auth state" function.
 *
 * <p>Flat rather than nested because Java records cannot extend a record; the
 * seven components are declared in the contract's field order so the serialized
 * JSON matches it exactly.
 *
 * <p><strong>There is deliberately no {@code refreshToken} component.</strong>
 * The refresh token travels only in an HttpOnly cookie; if it ever appears in a
 * response body that is a defect.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        OrganizationResponse currentOrganization,
        Role currentRole,
        List<MembershipResponse> memberships) {

    /** The only token type this API issues. */
    public static final String BEARER = "Bearer";

    public static AuthResponse of(String accessToken, long expiresIn, SessionResponse session) {
        return new AuthResponse(
                accessToken,
                BEARER,
                expiresIn,
                session.user(),
                session.currentOrganization(),
                session.currentRole(),
                session.memberships());
    }
}
