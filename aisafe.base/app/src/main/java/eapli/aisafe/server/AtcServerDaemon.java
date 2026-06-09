package eapli.aisafe.server;

import eapli.aisafe.remote.RemoteProtocol;

import java.net.Socket;

/**
 * US078 — ATCC remote access TCP server.
 * Listens on port {@value RemoteProtocol#ATC_PORT}.
 */
public class AtcServerDaemon extends AbstractTcpServer {

    public AtcServerDaemon() {
        super(RemoteProtocol.ATC_PORT, "US78/ATCC");
    }

    @Override
    protected Runnable createHandler(final Socket clientSocket) {
        return new AtcClientHandler(clientSocket);
    }
}