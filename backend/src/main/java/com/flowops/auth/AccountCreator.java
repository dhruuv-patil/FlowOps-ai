package com.flowops.auth;

import com.flowops.common.text.Slugs;
import com.flowops.domain.Organization;
import com.flowops.domain.OrganizationMember;
import com.flowops.domain.Role;
import com.flowops.domain.UserAccount;
import com.flowops.repository.OrganizationMemberRepository;
import com.flowops.repository.OrganizationRepository;
import com.flowops.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The atomic part of registration: user + organization + {@code OWNER} membership
 * in one transaction (contract §5.1). If any step fails, nothing is persisted.
 *
 * <p>A separate bean rather than a private method on {@link AuthService} because a
 * {@code @Transactional} self-invocation bypasses the proxy and would silently run
 * without a transaction — and because translating a constraint violation into a
 * {@code 409} needs to query the database, which is impossible inside the
 * transaction that just failed. {@code AuthService} therefore calls this across a
 * proxy boundary and handles the failure after the rollback has completed.
 */
@Component
class AccountCreator {

    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;

    AccountCreator(
            UserRepository users,
            OrganizationRepository organizations,
            OrganizationMemberRepository members) {
        this.users = users;
        this.organizations = organizations;
        this.members = members;
    }

    /** What registration produced, so the caller need not re-read any of it. */
    record Created(UserAccount user, Organization organization, OrganizationMember membership) {
    }

    @Transactional
    Created create(String email, String passwordHash, String fullName, String organizationName) {
        UserAccount user = users.save(UserAccount.create(email, passwordHash, fullName));

        String slug = Slugs.uniqueSlug(organizationName, organizations::existsBySlug);
        Organization organization =
                organizations.save(Organization.create(organizationName, slug));

        // The role is hardcoded here, never read from the request: the creator of an
        // organization is its OWNER and a client cannot ask to be anything else.
        OrganizationMember membership = members.save(
                OrganizationMember.create(user.getId(), organization.getId(), Role.OWNER));

        return new Created(user, organization, membership);
    }
}
