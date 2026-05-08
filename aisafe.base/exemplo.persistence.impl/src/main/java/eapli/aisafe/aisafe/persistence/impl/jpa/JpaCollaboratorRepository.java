package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.collaborator.domain.Collaborator;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.domain.CompanyIATA;
import eapli.exemplo.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of CollaboratorRepository.
 */
public class JpaCollaboratorRepository
        extends JpaAutoTxRepository<Collaborator, Long, Long>
        implements CollaboratorRepository {

    public JpaCollaboratorRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    public JpaCollaboratorRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "id");
    }

    @Override
    public Iterable<Collaborator> findByCompanyId(final CompanyIATA companyId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("iata", companyId.toString());
        return match("e.companyId.iataCode = :iata", params);
    }

    @Override
    public Iterable<Collaborator> findAllActive() {
        return match("e.active = true");
    }

    @Override
    public Optional<Collaborator> findBySystemUser(final SystemUser systemUser) {
        final Map<String, Object> params = new HashMap<>();
        params.put("su", systemUser);
        return matchOne("e.systemUser = :su", params);
    }
}
