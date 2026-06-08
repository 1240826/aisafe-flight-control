package eapli.aisafe.server;

import eapli.aisafe.remote.RemoteProtocol;
import eapli.aisafe.remote.atc.RemoteAtcService;
import eapli.aisafe.usermanagement.domain.AISafeRoles;

import java.net.Socket;

class AtcClientHandler extends AbstractClientHandler {

    private final RemoteAtcService atcService;

    AtcClientHandler(final Socket clientSocket) {
        super(clientSocket, RemoteProtocol.SVC_ATC, AISafeRoles.ATC_COLLABORATOR);
        this.atcService = new RemoteAtcService();
    }

    @Override
    protected String handleCommand(final String cmd, final String[] fields) {
        return switch (cmd) {
            case RemoteProtocol.CMD_ADD_AIRCRAFT          -> doAddAircraft(fields);
            case RemoteProtocol.CMD_DECOMMISSION_AIRCRAFT -> doDecommissionAircraft(fields);
            case RemoteProtocol.CMD_LIST_FLEET            -> doListFleet(fields);
            case RemoteProtocol.CMD_CREATE_ROUTE          -> doCreateRoute(fields);
            case RemoteProtocol.CMD_DELETE_ROUTE          -> doDeleteRoute(fields);
            case RemoteProtocol.CMD_ADD_PILOT             -> doAddPilot(fields);
            case RemoteProtocol.CMD_LIST_PILOTS           -> doListPilots(fields);
            case RemoteProtocol.CMD_REMOVE_PILOT          -> doRemovePilot(fields);
            case RemoteProtocol.CMD_LIST_ROUTES           -> doListRoutes();
            default -> RemoteProtocol.err("Unknown command: " + cmd);
        };
    }

    // ── ADD_AIRCRAFT|regNumber|regCountry|modelCode|companyIata|crewMembers|date

    private String doAddAircraft(final String[] f) {
        if (f.length < 7) {
            return RemoteProtocol.err(
                    "Usage: ADD_AIRCRAFT|regNumber|regCountry|modelCode|companyIata|crewMembers|date");
        }
        try {
            final String result = atcService.addAircraft(f[1], f[2], f[3], f[4],
                    Integer.parseInt(f[5]), f[6]);
            return RemoteProtocol.ok(result);
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── DECOMMISSION_AIRCRAFT|regNumber|regCountry

    private String doDecommissionAircraft(final String[] f) {
        if (f.length < 3) {
            return RemoteProtocol.err("Usage: DECOMMISSION_AIRCRAFT|regNumber|regCountry");
        }
        try {
            return RemoteProtocol.ok(atcService.decommissionAircraft(f[1], f[2]));
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── LIST_FLEET  or  LIST_FLEET|companyIata

    private String doListFleet(final String[] f) {
        try {
            final String result = f.length >= 2
                    ? atcService.listFleetByCompany(f[1])
                    : atcService.listFleet();
            return RemoteProtocol.ok(result);
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── CREATE_ROUTE|routeName|companyIata|originCode|destCode

    private String doCreateRoute(final String[] f) {
        if (f.length < 5) {
            return RemoteProtocol.err(
                    "Usage: CREATE_ROUTE|routeName|companyIata|originCode|destCode");
        }
        try {
            return RemoteProtocol.ok(atcService.createRoute(f[1], f[2], f[3], f[4]));
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── DELETE_ROUTE|routeName|date

    private String doDeleteRoute(final String[] f) {
        if (f.length < 3) {
            return RemoteProtocol.err(
                    "Usage: DELETE_ROUTE|routeName|date(yyyy-mm-dd)");
        }
        try {
            return RemoteProtocol.ok(atcService.deleteRoute(f[1], f[2]));
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── ADD_PILOT|licenseNumber|companyIata|certDate|model1,model2,...

    private String doAddPilot(final String[] f) {
        if (f.length < 5) {
            return RemoteProtocol.err(
                    "Usage: ADD_PILOT|licenseNumber|companyIata|certDate|model1,model2,...");
        }
        try {
            return RemoteProtocol.ok(atcService.addPilot(f[1], f[2], f[3], f[4]));
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── LIST_PILOTS|companyIata

    private String doListPilots(final String[] f) {
        if (f.length < 2) {
            return RemoteProtocol.err("Usage: LIST_PILOTS|companyIata");
        }
        try {
            return RemoteProtocol.ok(atcService.listPilots(f[1]));
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── REMOVE_PILOT|licenseNumber

    private String doRemovePilot(final String[] f) {
        if (f.length < 2) {
            return RemoteProtocol.err("Usage: REMOVE_PILOT|licenseNumber");
        }
        try {
            return RemoteProtocol.ok(atcService.removePilot(f[1]));
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }

    // ── LIST_ROUTES

    private String doListRoutes() {
        try {
            return RemoteProtocol.ok(atcService.listRoutes());
        } catch (final Exception e) {
            return RemoteProtocol.err(e.getMessage());
        }
    }
}