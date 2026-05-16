package eapli.aisafe.aircraft.domain;

/**
 * Operational status of an aircraft.
 * Transition: ACTIVE → DECOMMISSIONED only.
 * US070, US071.
 */
public enum OperationalStatus {
    ACTIVE,
    DECOMMISSIONED
}
