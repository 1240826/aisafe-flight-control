package eapli.aisafe.server;

import eapli.aisafe.remote.RemoteProtocol;

import java.net.Socket;

/**
 * US044 — Weather Person remote access TCP server.
 * Listens on port {@value RemoteProtocol#WEATHER_PORT}.
 */
public class WeatherServerDaemon extends AbstractTcpServer {

    public WeatherServerDaemon() {
        super(RemoteProtocol.WEATHER_PORT, "US44/Weather");
    }

    @Override
    protected Runnable createHandler(final Socket clientSocket) {
        return new WeatherClientHandler(clientSocket);
    }
}
