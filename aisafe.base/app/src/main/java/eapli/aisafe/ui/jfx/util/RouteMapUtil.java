package eapli.aisafe.ui.jfx.util;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RouteMapUtil {

    private RouteMapUtil() {
    }

    public static void showRouteMap(final String routeName,
                                    final String originCode, final String originName,
                                    final double originLat, final double originLon,
                                    final String destCode, final String destName,
                                    final double destLat, final double destLon) {
        try {
            final var html = loadTemplate()
                    .replace("{{ROUTE_NAME}}", escape(routeName))
                    .replace("{{ORIGIN_CODE}}", escape(originCode))
                    .replace("{{ORIGIN_NAME}}", escape(originName != null ? originName : ""))
                    .replace("{{ORIGIN_LAT}}", String.valueOf(originLat))
                    .replace("{{ORIGIN_LON}}", String.valueOf(originLon))
                    .replace("{{DEST_CODE}}", escape(destCode))
                    .replace("{{DEST_NAME}}", escape(destName != null ? destName : ""))
                    .replace("{{DEST_LAT}}", String.valueOf(destLat))
                    .replace("{{DEST_LON}}", String.valueOf(destLon));

            final Path tempFile = Files.createTempFile("aisafe-route-", ".html");
            tempFile.toFile().deleteOnExit();
            Files.writeString(tempFile, html, StandardCharsets.UTF_8);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(tempFile.toUri());
            } else {
                final String os = System.getProperty("os.name").toLowerCase();
                final Runtime rt = Runtime.getRuntime();
                if (os.contains("win")) {
                    rt.exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", tempFile.toUri().toString()});
                } else if (os.contains("mac")) {
                    rt.exec(new String[]{"open", tempFile.toString()});
                } else {
                    rt.exec(new String[]{"xdg-open", tempFile.toString()});
                }
            }
        } catch (final Exception e) {
            System.err.println("[RouteMapUtil] Failed to open route map:");
            e.printStackTrace(System.err);
        }
    }

    private static String loadTemplate() throws IOException {
        try (InputStream is = RouteMapUtil.class.getResourceAsStream("/map/route-map.html")) {
            if (is == null) {
                throw new IOException("Map template not found at /map/route-map.html");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String escape(final String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");
    }
}