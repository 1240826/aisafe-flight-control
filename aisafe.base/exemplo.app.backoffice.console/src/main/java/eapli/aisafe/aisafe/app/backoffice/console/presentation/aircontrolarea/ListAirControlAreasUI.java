package eapli.aisafe.app.backoffice.console.presentation.aircontrolarea;

import eapli.aisafe.aircontrolarea.application.RegisterAirControlAreaController;
import eapli.aisafe.aircontrolarea.domain.AirControlArea;
import eapli.framework.presentation.console.AbstractListUI;
import eapli.framework.visitor.Visitor;

/**
 * UI to list Air Control Areas.
 */
@SuppressWarnings("squid:S106")
public class ListAirControlAreasUI extends AbstractListUI<AirControlArea> {

    private final RegisterAirControlAreaController controller =
            new RegisterAirControlAreaController();

    @Override
    public String headline() { return "Air Control Areas"; }

    @Override
    protected String emptyMessage() { return "No air control areas registered."; }

    @Override
    protected Iterable<AirControlArea> elements() {
        return controller.allAirControlAreas();
    }

    @Override
    protected Visitor<AirControlArea> elementPrinter() {
        return element -> System.out.printf("  %-12s %-30s [%.1f,%.1f] x [%.1f,%.1f]%n",
                element.code(), element.name(),
                element.minLat(), element.maxLat(),
                element.minLon(), element.maxLon());
    }

    @Override
    protected String elementName() { return "Air Control Area"; }

    @Override
    protected String listHeader() {
        return String.format("  %-12s %-30s %-20s", "CODE", "NAME", "BOUNDS (lat x lon)");
    }
}
