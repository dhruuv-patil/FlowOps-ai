package com.flowops.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowops.common.error.ApiException;
import com.flowops.common.error.ErrorCode;
import com.flowops.domain.Invitation;
import com.flowops.domain.OrganizationMember;
import com.flowops.domain.Role;
import com.flowops.domain.UserAccount;
import com.flowops.repository.AuthSessionRepository;
import com.flowops.repository.InvitationRepository;
import com.flowops.repository.OrganizationMemberRepository;
import com.flowops.repository.OrganizationRepository;
import com.flowops.repository.UserRepository;
import com.flowops.security.FlowOpsPrincipal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Authorization and tenant-isolation tests for the Slice 3 team/invitation surface of
 * {@link OrganizationService}. Repositories are mocked; the assertions target the
 * security-critical guards: you cannot touch your own membership here, an admin can
 * never mint or modify an owner, the last owner is never demoted or removed, another
 * tenant's member is indistinguishable from absent, and an invitation cannot enroll an
 * account whose email does not match — every failure path is the opaque code the
 * contract mandates.
 */
class OrganizationServiceTeamTest {

    private final OrganizationRepository organizations = mock(OrganizationRepository.class);
    private final OrganizationMemberRepository members = mock(OrganizationMemberRepository.class);
    private final AuthSessionRepository sessions = mock(AuthSessionRepository.class);
    private final OrganizationCreator organizationCreator = mock(OrganizationCreator.class);
    private final com.flowops.auth.SessionAssembler sessionAssembler =
            mock(com.flowops.auth.SessionAssembler.class);
    private final UserRepository users = mock(UserRepository.class);
    private final InvitationRepository invitations = mock(InvitationRepository.class);
    private final InvitationTokens invitationTokens = mock(InvitationTokens.class);
    private final com.flowops.audit.AuditService audit = mock(com.flowops.audit.AuditService.class);

    private final OrganizationService service = new OrganizationService(
            organizations, members, sessions, organizationCreator, sessionAssembler, users,
            invitations, invitationTokens, audit);

    private final UUID orgId = UUID.randomUUID();

    private FlowOpsPrincipal principal(UUID userId, Role role) {
        return new FlowOpsPrincipal(userId, UUID.randomUUID(), orgId, role, "actor@example.com");
    }

    private OrganizationMember memberOf(UUID userId, Role role) {
        return OrganizationMember.create(userId, orgId, role);
    }

    private void stubActor(UUID actorId, Role role) {
        when(members.findByUserIdAndOrganizationId(actorId, orgId))
                .thenReturn(Optional.of(memberOf(actorId, role)));
    }

    /* -------------------------------------------------- changeMemberRole guards */

    @Test
    void cannotChangeOwnRole() {
        UUID actorId = UUID.randomUUID();
        stubActor(actorId, Role.ADMIN);

        assertThatThrownBy(() ->
                        service.changeMemberRole(principal(actorId, Role.ADMIN), actorId, Role.MEMBER))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.CANNOT_MODIFY_SELF);
    }

    @Test
    void changingAnUnknownOrCrossTenantMemberIsMemberNotFound() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        stubActor(actorId, Role.ADMIN);
        when(members.findByUserIdAndOrganizationId(targetId, orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        service.changeMemberRole(principal(actorId, Role.ADMIN), targetId, Role.MEMBER))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void adminCannotGrantOwner() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        stubActor(actorId, Role.ADMIN);
        when(members.findByUserIdAndOrganizationId(targetId, orgId))
                .thenReturn(Optional.of(memberOf(targetId, Role.MEMBER)));

        assertThatThrownBy(() ->
                        service.changeMemberRole(principal(actorId, Role.ADMIN), targetId, Role.OWNER))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.FORBIDDEN_ROLE);
    }

    @Test
    void adminCannotModifyAnExistingOwner() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        stubActor(actorId, Role.ADMIN);
        when(members.findByUserIdAndOrganizationId(targetId, orgId))
                .thenReturn(Optional.of(memberOf(targetId, Role.OWNER)));

        assertThatThrownBy(() ->
                        service.changeMemberRole(principal(actorId, Role.ADMIN), targetId, Role.MEMBER))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.FORBIDDEN_ROLE);
    }

    @Test
    void theLastOwnerCannotBeDemoted() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        stubActor(actorId, Role.OWNER);
        when(members.findByUserIdAndOrganizationId(targetId, orgId))
                .thenReturn(Optional.of(memberOf(targetId, Role.OWNER)));
        when(members.countByOrganizationIdAndRole(orgId, Role.OWNER)).thenReturn(1L);

        assertThatThrownBy(() ->
                        service.changeMemberRole(principal(actorId, Role.OWNER), targetId, Role.ADMIN))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.LAST_OWNER);
    }

    /* -------------------------------------------------- removeMember guards */

    @Test
    void cannotRemoveSelf() {
        UUID actorId = UUID.randomUUID();
        stubActor(actorId, Role.OWNER);

        assertThatThrownBy(() -> service.removeMember(principal(actorId, Role.OWNER), actorId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.CANNOT_MODIFY_SELF);
    }

    @Test
    void adminCannotRemoveAnOwner() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        stubActor(actorId, Role.ADMIN);
        when(members.findByUserIdAndOrganizationId(targetId, orgId))
                .thenReturn(Optional.of(memberOf(targetId, Role.OWNER)));

        assertThatThrownBy(() -> service.removeMember(principal(actorId, Role.ADMIN), targetId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.FORBIDDEN_ROLE);
        verify(members, never()).delete(any());
    }

    @Test
    void theLastOwnerCannotBeRemoved() {
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        stubActor(actorId, Role.OWNER);
        when(members.findByUserIdAndOrganizationId(targetId, orgId))
                .thenReturn(Optional.of(memberOf(targetId, Role.OWNER)));
        when(members.countByOrganizationIdAndRole(orgId, Role.OWNER)).thenReturn(1L);

        assertThatThrownBy(() -> service.removeMember(principal(actorId, Role.OWNER), targetId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.LAST_OWNER);
        verify(members, never()).delete(any());
    }

    /* -------------------------------------------------- invite guards */

    @Test
    void cannotInviteAsOwner() {
        UUID actorId = UUID.randomUUID();
        stubActor(actorId, Role.ADMIN);

        assertThatThrownBy(() -> service.invite(
                        principal(actorId, Role.ADMIN),
                        new InviteMemberRequest("new@example.com", Role.OWNER)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.INVITATION_INVALID);
        verify(invitations, never()).save(any());
    }

    @Test
    void cannotInviteAnExistingMember() {
        UUID actorId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        stubActor(actorId, Role.ADMIN);
        UserAccount existing = mock(UserAccount.class);
        when(existing.getId()).thenReturn(existingUserId);
        when(users.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(existing));
        when(members.findByUserIdAndOrganizationId(existingUserId, orgId))
                .thenReturn(Optional.of(memberOf(existingUserId, Role.MEMBER)));

        assertThatThrownBy(() -> service.invite(
                        principal(actorId, Role.ADMIN),
                        new InviteMemberRequest("member@example.com", Role.MEMBER)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.INVITATION_INVALID);
        verify(invitations, never()).save(any());
    }

    /* -------------------------------------------------- acceptInvitation isolation */

    @Test
    void acceptingWithAMismatchedEmailIsOpaqueNotFound() {
        UUID callerId = UUID.randomUUID();
        Invitation invitation = Invitation.create(
                orgId, "invited@example.com", Role.MEMBER, "hash", "int", UUID.randomUUID(),
                java.time.Instant.now().plusSeconds(3600));
        when(invitationTokens.hash("tok")).thenReturn("hash");
        when(invitations.findByTokenHash("hash")).thenReturn(Optional.of(invitation));
        UserAccount caller = mock(UserAccount.class);
        when(caller.getEmail()).thenReturn("someoneelse@example.com");
        when(users.findById(callerId)).thenReturn(Optional.of(caller));

        assertThatThrownBy(() -> service.acceptInvitation(principal(callerId, Role.MEMBER), "tok"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.INVITATION_NOT_FOUND);
        verify(members, never()).save(any());
    }

    @Test
    void unknownTokenIsOpaqueNotFound() {
        UUID callerId = UUID.randomUUID();
        when(invitationTokens.hash(any())).thenReturn("nope");
        when(invitations.findByTokenHash("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptInvitation(principal(callerId, Role.MEMBER), "tok"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.INVITATION_NOT_FOUND);
    }

    @Test
    void listMembersConfirmsMembershipFirst() {
        UUID actorId = UUID.randomUUID();
        when(members.findByUserIdAndOrganizationId(actorId, orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listMembers(principal(actorId, Role.MEMBER)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo(ErrorCode.FORBIDDEN_ROLE);
        verify(members, never()).findMemberRows(any());
    }

    @Test
    void listInvitationsReturnsMappedRows() {
        UUID actorId = UUID.randomUUID();
        stubActor(actorId, Role.ADMIN);
        Invitation invitation = Invitation.create(
                orgId, "a@example.com", Role.MEMBER, "h", "int", actorId,
                java.time.Instant.now().plusSeconds(3600));
        when(invitations.findByOrganizationIdOrderByCreatedAtDesc(orgId))
                .thenReturn(List.of(invitation));

        assertThat(service.listInvitations(principal(actorId, Role.ADMIN)).invitations())
                .hasSize(1)
                .allSatisfy(r -> assertThat(r.email()).isEqualTo("a@example.com"));
    }
}
