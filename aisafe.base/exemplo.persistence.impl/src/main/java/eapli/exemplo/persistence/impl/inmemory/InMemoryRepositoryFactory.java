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
package eapli.exemplo.persistence.impl.inmemory;

import eapli.aisafe.aircontrolarea.repositories.AirControlAreaRepository;
import eapli.aisafe.aircraft.repositories.AircraftRepository;
import eapli.aisafe.aircraftmodel.repositories.AircraftModelRepository;
import eapli.aisafe.airport.repositories.AirportRepository;
import eapli.aisafe.collaborator.repositories.CollaboratorRepository;
import eapli.aisafe.company.repositories.AirTransportCompanyRepository;
import eapli.aisafe.enginemodel.repositories.EngineModelRepository;
import eapli.aisafe.manufacturer.repositories.ManufacturerRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryAirControlAreaRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryAirTransportCompanyRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryAircraftModelRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryAircraftRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryAirportRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryCollaboratorRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryEngineModelRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryManufacturerRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryUserSecurityProfileRepository;
import eapli.aisafe.persistence.impl.inmemory.InMemoryWeatherDataRepository;
import eapli.aisafe.usermanagement.repositories.UserSecurityProfileRepository;
import eapli.aisafe.weatherdata.repositories.WeatherDataRepository;
import eapli.exemplo.infrastructure.persistence.RepositoryFactory;
import eapli.exemplo.usermanagement.domain.ExemploRoles;
import eapli.exemplo.usermanagement.domain.UserBuilderHelper;
import eapli.exemplo.utentemanagement.repositories.SignupRequestRepository;
import eapli.exemplo.utentemanagement.repositories.UtenteRepository;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.authz.domain.repositories.UserRepository;
import eapli.framework.infrastructure.authz.repositories.impl.inmemory.InMemoryUserRepository;

/**
 *
 * Created by nuno on 20/03/16.
 */
public class InMemoryRepositoryFactory implements RepositoryFactory {

	@Override
	public UserRepository users(final TransactionalContext tx) {
		final var repo = new InMemoryUserRepository();
		// ensure we have at least a power user to be able to use the application
		final var userBuilder = UserBuilderHelper.builder();
		userBuilder.withUsername("poweruser").withPassword("Password1").withName("joe", "power")
				.withEmail("joe@email.org").withRoles(ExemploRoles.POWER_USER);
		final var newUser = userBuilder.build();
		repo.save(newUser);
		return repo;
	}

	@Override
	public UserRepository users() {
		return users(null);
	}

	@Override
	public UtenteRepository utentes(final TransactionalContext tx) {

		return new InMemoryUtenteRepository();
	}

	@Override
	public UtenteRepository utentes() {
		return utentes(null);
	}

	@Override
	public SignupRequestRepository signupRequests() {
		return signupRequests(null);
	}

	@Override
	public SignupRequestRepository signupRequests(final TransactionalContext tx) {
		return new InMemorySignupRequestRepository();
	}

	@Override
	public TransactionalContext newTransactionalContext() {
		// in memory does not support transactions...
		return null;
	}

	// ── AISafe repositories ──────────────────────────────────────────────────

	@Override
	public AirControlAreaRepository airControlAreas() {
		return new InMemoryAirControlAreaRepository();
	}

	@Override
	public AirTransportCompanyRepository airTransportCompanies() {
		return new InMemoryAirTransportCompanyRepository();
	}

	@Override
	public AircraftRepository aircraft() {
		return new InMemoryAircraftRepository();
	}

	@Override
	public AircraftModelRepository aircraftModels() {
		return new InMemoryAircraftModelRepository();
	}

	@Override
	public AirportRepository airports() {
		return new InMemoryAirportRepository();
	}

	@Override
	public CollaboratorRepository collaborators() {
		return new InMemoryCollaboratorRepository();
	}

	@Override
	public EngineModelRepository engineModels() {
		return new InMemoryEngineModelRepository();
	}

	@Override
	public ManufacturerRepository manufacturers() {
		return new InMemoryManufacturerRepository();
	}

	@Override
	public WeatherDataRepository weatherData() {
		return new InMemoryWeatherDataRepository();
	}

	@Override
	public UserSecurityProfileRepository userSecurityProfiles() {
		return new InMemoryUserSecurityProfileRepository();
	}

}
