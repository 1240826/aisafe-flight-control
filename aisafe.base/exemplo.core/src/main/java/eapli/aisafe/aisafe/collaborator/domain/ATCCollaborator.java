package eapli.aisafe.collaborator.domain;

import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * ATC Collaborator (Air Traffic Controller).
 * Employed by an AirTransportCompany (companyId non-null).
 * No air control area (areaCode is null).
 * US061.
 */
@Entity
@DiscriminatorValue("ATC")
public class ATCCollaborator extends Collaborator {

    public ATCCollaborator(final SystemUser systemUser, final String name,
                           final String position, final SecurityClearance securityClearance,
                           final SkillsAssessment skillsAssessment,
                           final CompanyIATA companyId) {
        super(systemUser, name, position, securityClearance, skillsAssessment, companyId, null);
        Preconditions.noneNull(companyId);
    }

    protected ATCCollaborator() {
        // for ORM
    }
}
