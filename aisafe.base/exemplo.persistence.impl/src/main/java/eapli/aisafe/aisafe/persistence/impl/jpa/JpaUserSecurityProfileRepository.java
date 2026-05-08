package eapli.aisafe.persistence.impl.jpa;

import eapli.aisafe.usermanagement.domain.UserSecurityProfile;
import eapli.aisafe.usermanagement.repositories.UserSecurityProfileRepository;
import eapli.exemplo.Application;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;

import java.util.Optional;

/**
 * JPA implementation of UserSecurityProfileRepository.
 */
public class JpaUserSecurityProfileRepository
        extends JpaAutoTxRepository<UserSecurityProfile, String, String>
        implements UserSecurityProfileRepository {

    public JpaUserSecurityProfileRepository(final TransactionalContext autoTx) {
        super(autoTx, "username");
    }

    public JpaUserSecurityProfileRepository(final String puName) {
        super(puName, Application.settings().getExtendedPersistenceProperties(), "username");
    }

    @Override
    public Optional<UserSecurityProfile> findByUsername(final String username) {
        return findById(username);
    }
}
