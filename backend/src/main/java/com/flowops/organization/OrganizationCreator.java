package com.flowops.organization;

import com.flowops.common.text.Slugs;
import com.flowops.domain.Organization;
import com.flowops.domain.OrganizationMember;
import com.flowops.domain.Role;
import com.flowops.repository.OrganizationMemberRepository;
import com.flowops.repository.OrganizationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The atomic part of creating an additional organization: the org plus the
 * caller's {@code OWNER} membership in one transaction (contract §5.9). Mirrors
 * {@code AccountCreator} — a separate bean so its {@code @Transactional} runs
 * across a proxy boundary and a slug-race rollback does not poison the retry.
 */
@Component
class OrganizationCreator {

    private final OrganizationRepository organizations;
    private final OrganizationMemberRepository members;

    OrganizationCreator(
            OrganizationRepository organizations, OrganizationMemberRepository members) {
        this.organizations = organizations;
        this.members = members;
    }

    record Created(Organization organization, OrganizationMember membership) {
    }

    @Transactional
    Created create(java.util.UUID userId, String organizationName) {
        String slug = Slugs.uniqueSlug(organizationName, organizations::existsBySlug);
        Organization organization =
                organizations.save(Organization.create(organizationName, slug));

        // OWNER is hardcoded: the creator of an organization owns it, and no client
        // field can ask for anything else.
        OrganizationMember membership = members.save(
                OrganizationMember.create(userId, organization.getId(), Role.OWNER));

        return new Created(organization, membership);
    }
}
