package eapli.aisafe.simulation.domain;

/**
 * Outcome of a SCOMP simulation run stored in the system.
 */
public enum ValidationResult {

    /** The simulation passed all safety checks. */
    PASSED,

    /** The simulation failed one or more safety checks. */
    FAILED,

    /** The simulation has been recorded but not yet assessed. */
    PENDING
}
