package eapli.aisafe.server;

import eapli.aisafe.remote.RemoteProtocol;

import java.net.Socket;

/**
 * US086 — Pilot (Flight Control Operator) remote access TCP server.
 * Listens on port {@value RemoteProtocol#PILOT_PORT}.
 */
public class PilotServerDaemon extends AbstractTcpServer {

    public PilotServerDaemon() {
        super(RemoteProtocol.PILOT_PORT, "US86/Pilot");
    }

    @Override
    protected Runnable createHandler(final Socket clientSocket) {
        return new PilotClientHandler(clientSocket);
    }
}
