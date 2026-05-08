package eapli.aisafe.collaborator.domain;

import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Weather Person (meteorologist responsible for an Air Control Area).
 * areaCode non-null; companyId is null (not employed by a company).
 * US061.
 */
@Entity
@DiscriminatorValue("WEATHER")
public class WeatherPerson extends Collaborator {

    public WeatherPerson(final SystemUser systemUser, final String name,
                         final String position, final SecurityClearance securityClearance,
                         final SkillsAssessment skillsAssessment,
                         final AreaCode areaCode) {
        super(systemUser, name, position, securityClearance, skillsAssessment, null, areaCode);
        Preconditions.noneNull(areaCode);
    }

    protected WeatherPerson() {
        // for ORM
    }
}
