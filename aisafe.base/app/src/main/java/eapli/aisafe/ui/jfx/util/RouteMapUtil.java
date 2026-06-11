package eapli.aisafe.ui.jfx.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class RouteMapUtil {

    private static final int TILE_SIZE = 256;

    private RouteMapUtil() {
    }

    public static void showRouteMap(final String routeName,
                                     final String originCode, final String originName,
                                     final double originLat, final double originLon,
                                     final String destCode, final String destName,
                                     final double destLat, final double destLon) {
        final double centerLat = (originLat + destLat) / 2;
        final double centerLon = (originLon + destLon) / 2;
        final int osmZoom = calcZoom(originLat, originLon, destLat, destLon);

        final Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Route Map - " + routeName);
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        stage.setWidth(830);
        stage.setHeight(590);

        final Label title = new Label(routeName + "  (" + originCode + " \u2192 " + destCode + ")");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#f0f6fc"));

        final ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(32, 32);

        final Label loadingLabel = new Label("Loading map tiles...");
        loadingLabel.setTextFill(Color.web("#8b949e"));

        final ImageView mapView = new ImageView();
        mapView.setPreserveRatio(true);

        final Canvas canvas = new Canvas();
        canvas.setMouseTransparent(true);

        final StackPane loadingStack = new StackPane(spinner, loadingLabel);
        loadingStack.setAlignment(Pos.CENTER);

        final StackPane mapStack = new StackPane(mapView, canvas, loadingStack);
        mapStack.setStyle("-fx-background-color: #0d1117;");
        StackPane.setAlignment(loadingStack, Pos.CENTER);

        final Label zoomLabel = new Label("100%");
        zoomLabel.setTextFill(Color.web("#8b949e"));
        zoomLabel.setFont(Font.font("System", 11));

        final Button zoomInBtn = new Button("+");
        zoomInBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; " +
                "-fx-border-color: #30363d; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-cursor: hand; -fx-font-size: 16; -fx-font-weight: bold; -fx-min-width: 32; -fx-min-height: 28;");

        final Button zoomOutBtn = new Button("\u2212");
        zoomOutBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; " +
                "-fx-border-color: #30363d; -fx-border-radius: 4; -fx-background-radius: 4; " +
                "-fx-cursor: hand; -fx-font-size: 16; -fx-font-weight: bold; -fx-min-width: 32; -fx-min-height: 28;");

        final Button resetBtn = new Button("Reset");
        resetBtn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; " +
                "-fx-border-color: #30363d; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;");

        final Label legO = new Label("\u25cf " + originCode + (originName != null && !originName.isEmpty() ? " - " + originName : ""));
        legO.setTextFill(Color.LIME);
        legO.setFont(Font.font("System", FontWeight.BOLD, 12));

        final Label legD = new Label("\u25cf " + destCode + (destName != null && !destName.isEmpty() ? " - " + destName : ""));
        legD.setTextFill(Color.RED);
        legD.setFont(Font.font("System", FontWeight.BOLD, 12));

        final Label coordsLbl = new Label(String.format("%.4f, %.4f  \u2192  %.4f, %.4f", originLat, originLon, destLat, destLon));
        coordsLbl.setTextFill(Color.web("#484f58"));
        coordsLbl.setFont(Font.font("System", 10));

        final Button close = new Button("Close");
        close.setStyle("-fx-background-color: #21262d; -fx-text-fill: #e6edf3; " +
                "-fx-border-color: #30363d; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand;");
        close.setOnAction(e -> stage.close());

        final HBox zoomControls = new HBox(4, zoomOutBtn, zoomInBtn, resetBtn, zoomLabel);
        zoomControls.setAlignment(Pos.CENTER_LEFT);

        final HBox legendRow = new HBox(24, legO, legD);
        legendRow.setAlignment(Pos.CENTER);

        final HBox bottom = new HBox(16, zoomControls, legendRow, coordsLbl, close);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10, 0, 0, 0));

        final BorderPane root = new BorderPane();
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #0d1117;");

        root.setTop(title);
        root.setCenter(mapStack);
        root.setBottom(bottom);

        BorderPane.setMargin(title, new Insets(0, 0, 8, 0));

        final Scene scene = new Scene(root, 830, 590);
        scene.getStylesheets().add(RouteMapUtil.class.getResource("/styles/dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.show();

        final double[] currentScale = {1.0};
        final double[] panX = {0.0};
        final double[] panY = {0.0};
        final double[] dragStartX = {0.0};
        final double[] dragStartY = {0.0};
        final boolean[] dragging = {false};

        final Runnable updateZoom = () -> {
            final double s = currentScale[0];
            zoomLabel.setText(Math.round(s * 100) + "%");
            mapView.setScaleX(s);
            mapView.setScaleY(s);
            canvas.setScaleX(s);
            canvas.setScaleY(s);
            canvas.setTranslateX(panX[0]);
            canvas.setTranslateY(panY[0]);
            mapView.setTranslateX(panX[0]);
            mapView.setTranslateY(panY[0]);
        };

        final java.util.function.BiConsumer<Double, double[]> zoom = (factor, scaleBounds) -> {
            final double oldS = currentScale[0];
            final double newS = Math.max(scaleBounds[0], Math.min(oldS * factor, scaleBounds[1]));
            final double f = newS / oldS;
            final double vw = mapStack.getWidth();
            final double vh = mapStack.getHeight();
            if (vw > 0 && vh > 0) {
                panX[0] = vw / 2 - (vw / 2 - panX[0]) * f;
                panY[0] = vh / 2 - (vh / 2 - panY[0]) * f;
            }
            currentScale[0] = newS;
            updateZoom.run();
        };

        zoomInBtn.setOnAction(e -> zoom.accept(1.5, new double[]{0.25, 4.0}));
        zoomOutBtn.setOnAction(e -> zoom.accept(1.0 / 1.5, new double[]{0.25, 4.0}));

        resetBtn.setOnAction(e -> {
            currentScale[0] = 1.0;
            panX[0] = 0;
            panY[0] = 0;
            updateZoom.run();
        });

        mapStack.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX[0] = e.getSceneX();
                dragStartY[0] = e.getSceneY();
                dragging[0] = true;
                mapStack.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            }
        });

        mapStack.setOnMouseDragged(e -> {
            if (dragging[0]) {
                panX[0] += e.getSceneX() - dragStartX[0];
                panY[0] += e.getSceneY() - dragStartY[0];
                dragStartX[0] = e.getSceneX();
                dragStartY[0] = e.getSceneY();
                updateZoom.run();
            }
        });

        mapStack.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragging[0] = false;
                mapStack.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });

        mapStack.setOnScroll(e -> {
            zoom.accept(e.getDeltaY() > 0 ? 1.2 : 1.0 / 1.2, new double[]{0.25, 4.0});
            e.consume();
        });

        final double[] initialSize = new double[2];

        new Thread(() -> {
            loadTiles(osmZoom, centerLat, centerLon,
                    originLat, originLon, originCode,
                    destLat, destLon, destCode,
                    mapView, canvas, loadingStack,
                    mapStack, stage, initialSize);
        }).start();
    }

    private static void loadTiles(final int zoom, final double centerLat, final double centerLon,
                                   final double oLat, final double oLon, final String oCode,
                                   final double dLat, final double dLon, final String dCode,
                                   final ImageView mapView, final Canvas canvas,
                                   final StackPane loadingStack,
                                   final StackPane mapStack, final Stage stage,
                                   final double[] initialSize) {
        try {
            final double n = Math.pow(2, zoom);
            final int cxTile = (int) Math.floor((centerLon + 180) / 360 * n);
            final int cyTile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(centerLat)) +
                    1 / Math.cos(Math.toRadians(centerLat))) / Math.PI) / 2 * n);

            final int tilesAcross = (int) Math.ceil(800.0 / TILE_SIZE) + 1;
            final int tilesDown = (int) Math.ceil(500.0 / TILE_SIZE) + 1;
            final int halfX = tilesAcross / 2;
            final int halfY = tilesDown / 2;

            final Image[][] tileImages = new Image[tilesAcross][tilesDown];
            final HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            for (int tx = 0; tx < tilesAcross; tx++) {
                for (int ty = 0; ty < tilesDown; ty++) {
                    final int tileX = cxTile + tx - halfX;
                    final int tileY = cyTile + ty - halfY;
                    final String tileUrl = String.format("https://tile.openstreetmap.org/%d/%d/%d.png", zoom, tileX, tileY);
                    try {
                        final HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(tileUrl))
                                .timeout(Duration.ofSeconds(5))
                                .GET().build();
                        final HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                        if (resp.statusCode() == 200) {
                            tileImages[tx][ty] = new Image(new ByteArrayInputStream(resp.body()));
                        }
                    } catch (final Exception ignored) {
                    }
                }
            }

            final int fullW = tilesAcross * TILE_SIZE;
            final int fullH = tilesDown * TILE_SIZE;
            final WritableImage combined = new WritableImage(fullW, fullH);
            for (int tx = 0; tx < tilesAcross; tx++) {
                for (int ty = 0; ty < tilesDown; ty++) {
                    final Image tile = tileImages[tx][ty];
                    if (tile != null) {
                        final PixelReader pr = tile.getPixelReader();
                        if (pr != null) {
                            combined.getPixelWriter().setPixels(
                                    tx * TILE_SIZE, ty * TILE_SIZE, TILE_SIZE, TILE_SIZE, pr, 0, 0);
                        }
                    }
                }
            }

            Platform.runLater(() -> {
                final double w = stage.getWidth() - 60;
                final double h = stage.getHeight() - 160;
                initialSize[0] = w > 100 ? w : 800;
                initialSize[1] = h > 100 ? h : 500;

                mapView.setImage(combined);
                mapView.setFitWidth(initialSize[0]);
                mapView.setFitHeight(initialSize[1]);

                canvas.setWidth(initialSize[0]);
                canvas.setHeight(initialSize[1]);

                final double scaleX = initialSize[0] / fullW;
                final double scaleY = initialSize[1] / fullH;

                final double[] oPix = latLonToTilePixel(oLat, oLon, zoom, cxTile, cyTile, halfX, halfY, scaleX, scaleY);
                final double[] dPix = latLonToTilePixel(dLat, dLon, zoom, cxTile, cyTile, halfX, halfY, scaleX, scaleY);

                final GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.clearRect(0, 0, initialSize[0], initialSize[1]);

                gc.setStroke(Color.web("#58a6ff", 0.9));
                gc.setLineWidth(3);
                gc.setLineDashes(10, 8);
                gc.strokeLine(oPix[0], oPix[1], dPix[0], dPix[1]);
                gc.setLineDashes(null);

                drawMapMarker(gc, oPix[0], oPix[1], Color.LIME, oCode);
                drawMapMarker(gc, dPix[0], dPix[1], Color.RED, dCode);

                loadingStack.setVisible(false);
                loadingStack.setManaged(false);
            });
        } catch (final Exception e) {
            Platform.runLater(() -> {
                final double w = stage.getWidth() - 60;
                final double h = stage.getHeight() - 160;
                final double fw = w > 100 ? w : 800;
                final double fh = h > 100 ? h : 500;

                canvas.setWidth(fw);
                canvas.setHeight(fh);

                loadingStack.setVisible(false);
                loadingStack.setManaged(false);

                mapView.setVisible(false);
                drawFallbackDiagram(canvas.getGraphicsContext2D(), oLat, oLon, oCode, dLat, dLon, dCode, fw, fh);
            });
        }
    }

    private static double[] latLonToTilePixel(final double lat, final double lon,
                                               final int zoom, final int cxTile, final int cyTile,
                                               final int halfX, final int halfY,
                                               final double scaleX, final double scaleY) {
        final double n = Math.pow(2, zoom);
        final double px = (lon + 180) / 360 * n;
        final double py = (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * n;
        final double tileX = (px - (cxTile - halfX)) * TILE_SIZE * scaleX;
        final double tileY = (py - (cyTile - halfY)) * TILE_SIZE * scaleY;
        return new double[]{tileX, tileY};
    }

    private static int calcZoom(final double oLat, final double oLon,
                                 final double dLat, final double dLon) {
        final double dist = Math.sqrt(Math.pow(oLat - dLat, 2) + Math.pow(oLon - dLon, 2));
        if (dist < 0.5) return 9;
        if (dist < 1.5) return 8;
        if (dist < 4.0) return 7;
        if (dist < 10.0) return 6;
        return 5;
    }

    private static void drawMapMarker(final GraphicsContext gc, final double x, final double y,
                                       final Color color, final String code) {
        gc.setFill(color);
        gc.fillOval(x - 6, y - 6, 12, 12);
        gc.setStroke(Color.web("#ffffff", 0.6));
        gc.setLineWidth(2);
        gc.strokeOval(x - 8, y - 8, 16, 16);
        gc.setFill(Color.web("#e6edf3"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        gc.fillText(code, x + 12, y + 5);
    }

    private static void drawFallbackDiagram(final GraphicsContext gc,
                                             final double oLat, final double oLon, final String oCode,
                                             final double dLat, final double dLon, final String dCode,
                                             final double w, final double h) {
        if (w <= 10 || h <= 10) return;
        gc.setFill(Color.web("#0d1117"));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.web("#21262d"));
        gc.setLineWidth(0.5);
        for (double x = 0; x <= w; x += 50) gc.strokeLine(x, 0, x, h);
        for (double y = 0; y <= h; y += 50) gc.strokeLine(0, y, w, y);

        double minLat = Math.min(oLat, dLat), maxLat = Math.max(oLat, dLat);
        double minLon = Math.min(oLon, dLon), maxLon = Math.max(oLon, dLon);
        double latR = Math.max(maxLat - minLat, 0.5);
        double lonR = Math.max(maxLon - minLon, 0.5);
        double pLat = latR * 0.12, pLon = lonR * 0.12;
        double dmnLat = minLat - pLat, dmxLat = maxLat + pLat;
        double dmnLon = minLon - pLon, dmxLon = maxLon + pLon;
        double sx = w / (dmxLon - dmnLon);
        double sy = h / (dmxLat - dmnLat);

        final double ox = (oLon - dmnLon) * sx;
        final double oy = (dmxLat - oLat) * sy;
        final double dx = (dLon - dmnLon) * sx;
        final double dy = (dmxLat - dLat) * sy;

        gc.setStroke(Color.web("#58a6ff"));
        gc.setLineWidth(2.5);
        gc.setLineDashes(8, 6);
        gc.strokeLine(ox, oy, dx, dy);
        gc.setLineDashes(null);

        drawMapMarker(gc, ox, oy, Color.LIME, oCode);
        drawMapMarker(gc, dx, dy, Color.RED, dCode);

        gc.setFill(Color.web("#484f58"));
        gc.setFont(Font.font("System", 11));
        gc.fillText(String.format("%.4f,%.4f \u2192 %.4f,%.4f", oLat, oLon, dLat, dLon), 10, 20);
    }
}
