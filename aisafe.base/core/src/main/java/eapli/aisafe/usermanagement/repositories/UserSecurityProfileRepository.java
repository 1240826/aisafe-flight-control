package eapli.aisafe.usermanagement.repositories;

import eapli.aisafe.usermanagement.domain.UserSecurityProfile;
import eapli.framework.domain.repositories.DomainRepository;

import java.util.Optional;

/**
 * Repository for UserSecurityProfile.
 * US030 / US031.
 */
public interface UserSecurityProfileRepository extends DomainRepository<String, UserSecurityProfile> {

    Optional<UserSecurityProfile> findByUsername(String username);
}
