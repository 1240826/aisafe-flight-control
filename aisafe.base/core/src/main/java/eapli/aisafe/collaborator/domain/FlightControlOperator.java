package eapli.aisafe.collaborator.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Flight Control Operator — responsible for one Air Control Area.
 * Client clarification: "FCO responsible for just one ACA".
 * areaCode non-null; companyId is null (FCO is not employed by a company).
 * US061.
 */
@Entity
@DiscriminatorValue("FCO")
public class FlightControlOperator extends Collaborator {

    public FlightControlOperator(final SystemUser systemUser, final String name,
                                 final String position, final SecurityClearance securityClearance,
                                 final SkillsAssessment skillsAssessment,
                                 final AreaCode areaCode) {
        super(systemUser, name, position, securityClearance, skillsAssessment, null, areaCode);
        Preconditions.noneNull(areaCode);
    }

    protected FlightControlOperator() {
        // for ORM
    }
}
