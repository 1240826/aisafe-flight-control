package eapli.exemplo.infrastructure.bootstrapers;

import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.aisafe.aircontrolarea.domain.AreaCode;
import eapli.aisafe.aircontrolarea.domain.AreaName;
import eapli.aisafe.manufacturer.domain.Manufacturer;
import eapli.exemplo.infrastructure.persistence.PersistenceContext;
import eapli.framework.actions.Action;
import eapli.framework.domain.repositories.ConcurrencyException;
import eapli.framework.domain.repositories.IntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seeds demo domain data for AISafe:
 * - Sample manufacturers (Boeing, Airbus, CFM International, etc.)
 * - Sample Air Control Areas (Lisboa ACC, Madrid ACC)
 * These are the minimum reference data needed for the system to be usable.
 */
@SuppressWarnings("squid:S106")
public class AISafeDemoDataBootstrapper implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(AISafeDemoDataBootstrapper.class);

    @Override
    public boolean execute() {
        bootstrapManufacturers();
        bootstrapAirControlAreas();
        return true;
    }

    private void bootstrapManufacturers() {
        saveManufacturer("Boeing", "United States");
        saveManufacturer("Airbus", "France");
        saveManufacturer("Embraer", "Brazil");
        saveManufacturer("CFM International", "United States/France");
        saveManufacturer("Pratt & Whitney", "United States");
        saveManufacturer("Rolls-Royce", "United Kingdom");
        saveManufacturer("GE Aviation", "United States");
    }

    private void saveManufacturer(final String name, final String country) {
        try {
            PersistenceContext.repositories().manufacturers().save(new Manufacturer(name, country));
            LOGGER.debug("Bootstrapped manufacturer: {}", name);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("Manufacturer already exists: {}", name);
        }
    }

    private void bootstrapAirControlAreas() {
        saveACA("ACC-LIS", "Lisboa ACC",
                36.0, 42.0, -12.0, -6.0, 14000);
        saveACA("ACC-MAD", "Madrid ACC",
                35.0, 44.0, -10.0, 5.0, 14000);
    }

    private void saveACA(final String code, final String name,
                          final double minLat, final double maxLat,
                          final double minLon, final double maxLon,
                          final int maxAlt) {
        try {
            PersistenceContext.repositories().airControlAreas().save(
                    new AirControlArea(AreaCode.valueOf(code), new AreaName(name),
                            minLat, maxLat, minLon, maxLon, maxAlt));
            LOGGER.debug("Bootstrapped ACA: {}", code);
        } catch (final IntegrityViolationException | ConcurrencyException e) {
            LOGGER.warn("ACA already exists: {}", code);
        }
    }
}
