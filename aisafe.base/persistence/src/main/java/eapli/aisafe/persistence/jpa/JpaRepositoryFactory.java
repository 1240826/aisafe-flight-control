/*
 * Copyright (c) 2013-2024 the original author or authors.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package eapli.aisafe.persistence.jpa;

import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.aisafe.simulation.repositories.SimulationRepository;
import eapli.aisafe.usermanagement.repositories.UserSecurityProfileRepository;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.aisafe.infrastructure.Application;
import eapli.aisafe.infrastructure.persistence.RepositoryFactory;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.repositories.impl.jpa.JpaAutoTxUserRepository;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import eapli.aisafe.flightroute.repositories.FlightRouteRepository;


/**
 *
 * Created by nuno on 21/03/16.
 */
public class JpaRepositoryFactory implements RepositoryFactory {

    @Override
    public UserRepository users(final TransactionalContext autoTx) {
        return new JpaAutoTxUserRepository(autoTx);
    }

    @Override
    public UserRepository users() {
        return new JpaAutoTxUserRepository(Application.settings().getPersistenceUnitName(),
                Application.settings().getExtendedPersistenceProperties());
    }

    @Override
    public TransactionalContext newTransactionalContext() {
        return JpaAutoTxRepository.buildTransactionalContext(Application.settings().getPersistenceUnitName(),
                Application.settings().getExtendedPersistenceProperties());
    }

    // ── AISafe repositories ──────────────────────────────────────────────────

    @Override
    public AirControlAreaRepository airControlAreas() {
        return new JpaAirControlAreaRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public AirTransportCompanyRepository airTransportCompanies() {
        return new JpaAirTransportCompanyRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public AircraftRepository aircraft() {
        return new JpaAircraftRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public AircraftModelRepository aircraftModels() {
        return new JpaAircraftModelRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public AirportRepository airports() {
        return new JpaAirportRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public CollaboratorRepository collaborators() {
        return new JpaCollaboratorRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public EngineModelRepository engineModels() {
        return new JpaEngineModelRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public ManufacturerRepository manufacturers() {
        return new JpaManufacturerRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public WeatherDataRepository weatherData() {
        return new JpaWeatherDataRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public SimulationRepository simulations() {
        return new JpaSimulationRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public UserSecurityProfileRepository userSecurityProfiles() {
        return new JpaUserSecurityProfileRepository(Application.settings().getPersistenceUnitName());
    }

    @Override
    public UserSecurityProfileRepository userSecurityProfiles(final TransactionalContext autoTx) {
        return new JpaUserSecurityProfileRepository(autoTx);
    }
    @Override
    public FlightRouteRepository flightRoutes() {
        return new JpaFlightRouteRepository(Application.settings().getPersistenceUnitName());
    }

}
