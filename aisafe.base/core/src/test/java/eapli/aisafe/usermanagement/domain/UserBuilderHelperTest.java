package eapli.aisafe.usermanagement.domain;

import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserBuilderHelperTest {

    @Test
    void ensureBuilderReturnsSystemUserBuilder() {
        final SystemUserBuilder builder = UserBuilderHelper.builder();
        assertNotNull(builder);
    }
}