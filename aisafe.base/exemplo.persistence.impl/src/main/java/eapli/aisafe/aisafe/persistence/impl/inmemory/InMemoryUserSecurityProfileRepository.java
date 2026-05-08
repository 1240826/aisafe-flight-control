package eapli.aisafe.persistence.impl.inmemory;

import eapli.aisafe.usermanagement.domain.UserSecurityProfile;
import eapli.aisafe.usermanagement.repositories.UserSecurityProfileRepository;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;

import java.util.Optional;

/**
 * InMemory implementation of UserSecurityProfileRepository.
 */
public class InMemoryUserSecurityProfileRepository
        extends InMemoryDomainRepository<UserSecurityProfile, String>
        implements UserSecurityProfileRepository {

    @Override
    public Optional<UserSecurityProfile> findByUsername(final String username) {
        return findById(username);
    }
}
