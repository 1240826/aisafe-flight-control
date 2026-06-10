package eapli.aisafe.ui.jfx.util;

import eapli.aisafe.ui.jfx.SceneManager;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class TableZoomUtil {

    private TableZoomUtil() {
    }

    public static void openZoom(final TableView table, final String title) {
        final TableView zoomTable = new TableView();
        zoomTable.setItems(table.getItems());
        zoomTable.setEditable(table.isEditable());
        zoomTable.setColumnResizePolicy(table.getColumnResizePolicy());
        zoomTable.getSelectionModel().setSelectionMode(table.getSelectionModel().getSelectionMode());
        zoomTable.setPlaceholder(table.getPlaceholder());
        zoomTable.getStyleClass().addAll(table.getStyleClass());
        zoomTable.setId("zoom-table");

        for (final Object colObj : table.getColumns()) {
            final TableColumn col = (TableColumn) colObj;
            final TableColumn newCol = new TableColumn(col.getText());
            newCol.setCellValueFactory(col.getCellValueFactory());
            newCol.setCellFactory(col.getCellFactory());
            newCol.setPrefWidth(col.getPrefWidth() * 1.5);
            newCol.setMinWidth(col.getMinWidth());
            newCol.setMaxWidth(col.getMaxWidth());
            newCol.setSortType(col.getSortType());
            newCol.setStyle(col.getStyle());
            newCol.setEditable(col.isEditable());
            newCol.setReorderable(col.isReorderable());
            newCol.setResizable(col.isResizable());
            newCol.setSortable(col.isSortable());
            newCol.setVisible(col.isVisible());
            newCol.getStyleClass().addAll(col.getStyleClass());
            zoomTable.getColumns().add(newCol);
        }

        zoomTable.getSortOrder().addAll(table.getSortOrder());

        final TextField searchField = new TextField();
        searchField.setPromptText("Search " + title.toLowerCase() + "...");
        searchField.getStyleClass().add("form-field");
        searchField.setStyle("-fx-pref-width: 300;");
        searchField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                zoomTable.setItems(table.getItems());
            } else {
                zoomTable.setItems(table.getItems().filtered(item -> {
                    for (final Object colObj : zoomTable.getColumns()) {
                        final TableColumn col = (TableColumn) colObj;
                        final Object cellVal = col.getCellData(item);
                        if (cellVal != null && cellVal.toString().toLowerCase().contains(val.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                }));
            }
        });

        final Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #f0f6fc;");

        final Label rowCount = new Label();
        rowCount.setStyle("-fx-font-size: 12px; -fx-text-fill: #8b949e;");
        rowCount.textProperty().bind(Bindings.createStringBinding(
                () -> zoomTable.getItems().size() + " row(s)",
                zoomTable.getItems()));

        final HBox header = new HBox(16, titleLabel, searchField, new Region(), rowCount);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #161b22; -fx-border-color: transparent transparent #21262d transparent; -fx-border-width: 0 0 1 0;");

        final HBox statusBar = new HBox(rowCount);
        statusBar.setPadding(new Insets(8, 20, 8, 20));
        statusBar.setStyle("-fx-background-color: #161b22; -fx-border-color: #21262d transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        statusBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        final BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d1117;");
        root.setTop(header);
        root.setCenter(zoomTable);
        root.setBottom(statusBar);

        final Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(table.getScene().getWindow());
        final Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add("/styles/dark-theme.css");
        final var iconStream = SceneManager.class.getResourceAsStream("/icons/aisafe-icon.png");
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }
        stage.setTitle(title + " — Zoom View");
        stage.setMaximized(true);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }
}