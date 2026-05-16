package eapli.aisafe.enginemodel.domain;

/**
 * Bootstrapped list of available aviation fuel types (US056, clarification §14).
 * fuelType on EngineModel stores the name String selected from this list.
 */
public final class FuelType {

    public static final String JET_A1   = "Jet-A1";
    public static final String AVGAS    = "AvGas 100LL";
    public static final String SAF      = "SAF";

    /** Ordered list shown in the UI. */
    public static final String[] ALL = { JET_A1, AVGAS, SAF };

    private FuelType() {}
}
