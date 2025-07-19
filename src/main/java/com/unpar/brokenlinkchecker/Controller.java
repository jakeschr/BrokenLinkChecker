package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.CheckStatus;
import com.unpar.brokenlinkchecker.model.LinkResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class Controller {

    @FXML private TextField urlField;
    @FXML private ComboBox<String> techChoiceBox;
    @FXML private Button checkButton;
    @FXML private Button stopButton;
    @FXML private Button exportButton;

    @FXML private Label statusLabel;
    @FXML private Label pageCountLabel;
    @FXML private Label linkCountLabel;
    @FXML private Label brokenCountLabel;
    @FXML private Pagination pagination;

    @FXML private TableView<LinkResult> resultTable;
    @FXML private TableColumn<LinkResult, Number> colNumber;
    @FXML private TableColumn<LinkResult, String> colBrokenLink;
    @FXML private TableColumn<LinkResult, String> colStatus;
    @FXML private TableColumn<LinkResult, String> colSourcePage;
    @FXML private TableColumn<LinkResult, String> colAnchorText;

    // OLD
    private final ObservableList<LinkResult> resultData = FXCollections.observableArrayList();
    // NEW
    private final ObservableList<LinkResult> allResults = FXCollections.observableArrayList();
    private final ObservableList<LinkResult> currentPageResults = FXCollections.observableArrayList();
    private CheckStatus currentStatus = CheckStatus.IDLE;

    private int totalPages = 0;
    private int totalLinks = 0;

    private static final int ROWS_PER_PAGE = 30;

    @FXML
    public void initialize() {
        // Hitung rasio total (misal: 3 + 15 + 40 + 17 + 25 = 100)
        // Binding width proporsional
        double totalRatio = 100.0;
        colNumber.prefWidthProperty().bind(resultTable.widthProperty().multiply(3 / totalRatio));
        colStatus.prefWidthProperty().bind(resultTable.widthProperty().multiply(15 / totalRatio));
        colBrokenLink.prefWidthProperty().bind(resultTable.widthProperty().multiply(40 / totalRatio));
        colAnchorText.prefWidthProperty().bind(resultTable.widthProperty().multiply(17 / totalRatio));
        colSourcePage.prefWidthProperty().bind(resultTable.widthProperty().multiply(25 / totalRatio));


        // Kolom nomor (index + 1)
        colNumber.setCellValueFactory(cell -> {
            return javafx.beans.binding.Bindings.createIntegerBinding(
                    () -> resultTable.getItems().indexOf(cell.getValue()) + 1
            );
        });


        techChoiceBox.setItems(FXCollections.observableArrayList("Jsoup", "Playwright"));
        techChoiceBox.getSelectionModel().selectFirst();

        colBrokenLink.setCellValueFactory(cell -> cell.getValue().brokenLinkProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colSourcePage.setCellValueFactory(cell -> cell.getValue().sourcePageProperty());
        colAnchorText.setCellValueFactory(cell -> cell.getValue().anchorTextProperty());

        resultTable.setItems(resultData);

        setStatus(CheckStatus.IDLE);
    }

    @FXML
    public void onCheckClick() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showAlert("URL kosong", "Silakan masukkan URL terlebih dahulu.");
            return;
        }

        // Set awal status
        setStatus(CheckStatus.CHECKING);
        resultData.clear();
        totalPages = 0;
        totalLinks = 0;
        updateCounts();

        // Dummy data simulasi streaming
        new Thread(() -> {
            try {
                for (int i = 1; i <= 150; i++) {
                    if (currentStatus == CheckStatus.STOPPED) break;

                    String dummyUrl = "https://example.com/broken-" + i;
                    LinkResult result = new LinkResult(dummyUrl, "404 Not Found", "https://example.com/page" + i, "klik di sini");

                    Platform.runLater(() -> {
                        resultData.add(result);
                        totalPages++;
                        totalLinks += 5; // misal 5 link per halaman
                        updateCounts();
                        statusLabel.setText("Memeriksa halaman ke-" + totalPages);
                    });

                    Thread.sleep(1000); // delay simulasi
                }

                Platform.runLater(() -> {
                    if (currentStatus != CheckStatus.STOPPED) {
                        setStatus(CheckStatus.COMPLETED);
                    }
                });
            } catch (InterruptedException e) {
                Platform.runLater(() -> setStatus(CheckStatus.ERROR));
            }
        }).start();
    }

    @FXML
    public void onStopClick() {
        setStatus(CheckStatus.STOPPED);
    }

    @FXML
    public void onExportClick() {
        showAlert("Export", "Fitur export belum diimplementasikan.");
    }

    private void setStatus(CheckStatus status) {
        this.currentStatus = status;
        switch (status) {
            case IDLE -> {
                statusLabel.setText("Belum mulai");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(true);
            }
            case CHECKING -> {
                statusLabel.setText("Sedang memeriksa...");
                checkButton.setDisable(true);
                stopButton.setDisable(false);
                exportButton.setDisable(true);
            }
            case COMPLETED -> {
                statusLabel.setText("Selesai ✔");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(false);
            }
            case STOPPED -> {
                statusLabel.setText("Dihentikan ⏹");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(resultData.isEmpty());
            }
            case ERROR -> {
                statusLabel.setText("Terjadi kesalahan ❌");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(true);
            }
        }
    }

    private void updateCounts() {
        pageCountLabel.setText(String.valueOf(totalPages));
        linkCountLabel.setText(String.valueOf(totalLinks));
        brokenCountLabel.setText(String.valueOf(
                resultData.stream().filter(l -> l.getStatus().startsWith("4") || l.getStatus().startsWith("5")).count()
        ));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
