package eapli.aisafe.collaborator.repositories;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.domain.repositories.DomainRepository;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;

import java.util.Optional;

/**
 * Repository for Collaborator aggregate hierarchy.
 * US061, US062, US063, US064.
 */
public interface CollaboratorRepository extends DomainRepository<Long, Collaborator> {

    /** US062: find all collaborators of a company. */
    Iterable<Collaborator> findByCompanyId(CompanyIATA companyId);

    /** US064: find only active collaborators. */
    Iterable<Collaborator> findAllActive();

    /** Find collaborator linked to the given SystemUser. */
    Optional<Collaborator> findBySystemUser(SystemUser systemUser);
}
