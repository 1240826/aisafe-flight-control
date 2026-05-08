package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainAutoNumberRepository;

import java.util.Optional;

public class InMemoryCollaboratorRepository
        extends InMemoryDomainAutoNumberRepository<Collaborator>
        implements CollaboratorRepository {

    @Override
    public Iterable<Collaborator> findByCompanyId(final CompanyIATA companyId) {
        return match(c -> c.companyId() != null && c.companyId().equals(companyId));
    }

    @Override
    public Iterable<Collaborator> findAllActive() {
        return match(Collaborator::isActive);
    }

    @Override
    public Optional<Collaborator> findBySystemUser(final SystemUser systemUser) {
        return matchOne(c -> c.systemUser().equals(systemUser));
    }
}
