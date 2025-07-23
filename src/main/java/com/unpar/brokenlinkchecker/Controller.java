package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.CheckStatus;
import com.unpar.brokenlinkchecker.model.LinkResult;

import com.unpar.brokenlinkchecker.service.Crawler;
import com.unpar.brokenlinkchecker.service.HttpStatusChecker;
import com.unpar.brokenlinkchecker.service.JsoupCrawler;
import com.unpar.brokenlinkchecker.service.LinkCheckerService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller utama untuk aplikasi BrokenLinkChecker.
 * Mengelola interaksi antara UI JavaFX (main.fxml) dan logika pemeriksaan tautan.
 */
public class Controller {

    // ===== Komponen UI dari main.fxml =====

    @FXML private TextField urlField; // Input URL awal yang akan dicek
    @FXML private ComboBox<String> techChoiceBox; // Pilihan teknologi yang digunakan (Jsoup atau Playwright)
    @FXML private Button checkButton; // Tombol untuk memulai pemeriksaan tautan
    @FXML private Button stopButton; // Tombol untuk menghentikan proses pemeriksaan
    @FXML private Button exportButton; // Tombol untuk mengekspor hasil (belum diimplementasikan)

    // Label-label untuk menampilkan ringkasan status
    @FXML private Label statusLabel;
    @FXML private Label pageCountLabel;
    @FXML private Label linkCountLabel;
    @FXML private Label brokenCountLabel;

    // Kontainer VBox untuk menampilkan pagination kustom
    @FXML private VBox customPagination;

    // Komponen tabel hasil pemeriksaan tautan
    @FXML private TableView<LinkResult> resultTable;
    @FXML private TableColumn<LinkResult, Number> colNumber;
    @FXML private TableColumn<LinkResult, String> colBrokenLink;
    @FXML private TableColumn<LinkResult, String> colStatus;
    @FXML private TableColumn<LinkResult, String> colSourcePage;
    @FXML private TableColumn<LinkResult, String> colAnchorText;

    // ===== Variabel internal aplikasi =====

    private LinkCheckerService linkCheckerService;


    // Menyimpan semua hasil link (seluruh data)
    private final ObservableList<LinkResult> allResults = FXCollections.observableArrayList();
    // Menyimpan hasil yang ditampilkan di halaman saat ini (pagination)
    private final ObservableList<LinkResult> currentPageResults = FXCollections.observableArrayList();

    // Data terkait pagination
    private int currentPage = 1;
    private int totalPageCount = 0;

    // Data statistik
    private int totalLinks = 0;
    private int totalPages = 0;

    // Status saat ini (IDLE, CHECKING, COMPLETED, dll.)
    private CheckStatus currentStatus = CheckStatus.IDLE;

    // ===== Konstanta Pagination =====
    private static final int MAX_PAGE_BUTTONS = 5; // Jumlah maksimal tombol halaman yang ditampilkan
    private static final int ROWS_PER_PAGE = 10;   // Jumlah baris per halaman
    private static final double PAGE_BUTTON_WIDTH = 40; // Lebar tombol halaman


    /**
     * Method ini dipanggil secara otomatis saat controller dimuat oleh JavaFX.
     * Bertugas untuk menginisialisasi komponen UI, seperti kolom tabel dan dropdown pilihan.
     */
    @FXML
    public void initialize() {
        // Atur lebar kolom tabel secara proporsional
        double totalRatio = 100.0;
        colNumber.prefWidthProperty().bind(resultTable.widthProperty().multiply(3 / totalRatio));
        colStatus.prefWidthProperty().bind(resultTable.widthProperty().multiply(15 / totalRatio));
        colBrokenLink.prefWidthProperty().bind(resultTable.widthProperty().multiply(40 / totalRatio));
        colAnchorText.prefWidthProperty().bind(resultTable.widthProperty().multiply(17 / totalRatio));
        colSourcePage.prefWidthProperty().bind(resultTable.widthProperty().multiply(25 / totalRatio));

        // Set kolom nomor baris dengan perhitungan dinamis berdasarkan pagination
        colNumber.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createIntegerBinding(() -> {
            int indexInPage = currentPageResults.indexOf(cell.getValue());
            return (currentPage - 1) * ROWS_PER_PAGE + indexInPage + 1;
        }));

        // Set pilihan teknologi yang tersedia
        techChoiceBox.setItems(FXCollections.observableArrayList("Jsoup", "Playwright"));
        techChoiceBox.getSelectionModel().selectFirst(); // Default: Jsoup

        // Hubungkan properti kolom tabel dengan data LinkResult
        colBrokenLink.setCellValueFactory(cell -> cell.getValue().brokenLinkProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colSourcePage.setCellValueFactory(cell -> cell.getValue().sourcePageProperty());
        colAnchorText.setCellValueFactory(cell -> cell.getValue().anchorTextProperty());

        // Tampilkan halaman pertama
        resultTable.setItems(currentPageResults);

        // Set status awal
        setStatus(CheckStatus.IDLE);
    }


//    @FXML
//    public void onCheckClick() {
//        String url = urlField.getText().trim();
//        if (url.isEmpty()) {
//            showAlert("Empty URL", "Please enter a URL first.");
//            return;
//        }
//
//        String selectedTech = techChoiceBox.getSelectionModel().getSelectedItem();
//
//        // Tentukan teknologi crawling yang digunakan
//        Crawler crawler;
//        if ("Jsoup".equalsIgnoreCase(selectedTech)) {
//            crawler = new JsoupCrawler();
//        } else {
//            showAlert("Error", "Playwright belum didukung di mode modular.");
//            return;
//        }
//
//        linkCheckerService = new LinkCheckerService(crawler, new HttpStatusChecker());
//
//        // Reset data tampilan
//        setStatus(CheckStatus.CHECKING);
//        allResults.clear();
//        currentPageResults.clear();
//        currentPage = 1;
//        totalLinks = 0;
//        totalPages = 0;
//        updateCurrentPage(0);
//        updateCustomPagination();
//        updateCounts();
//
//        // Jalankan proses di thread terpisah (agar UI tidak freeze)
//        new Thread(() -> {
//            try {
//                linkCheckerService.run(url, result -> {
//                    Platform.runLater(() -> {
//                        allResults.add(result);
//                        totalPageCount = (int) Math.ceil((double) allResults.size() / ROWS_PER_PAGE);
//                        updateCurrentPage(currentPage - 1);
//                        updateCustomPagination();
//                        updateCounts();
//                    });
//                });
//
//                Platform.runLater(() -> setStatus(CheckStatus.COMPLETED));
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                Platform.runLater(() -> setStatus(CheckStatus.ERROR));
//            }
//        }).start();
//    }


    @FXML
    public void onCheckClick() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showAlert("Empty URL", "Please enter a URL first.");
            return;
        }

        setStatus(CheckStatus.CHECKING);
        totalPages = 0;
        totalLinks = 0;

        allResults.clear();
        currentPageResults.clear();

        new Thread(() -> {
            try {
                for (int i = 1; i <= 150; i++) {
                    if (currentStatus == CheckStatus.STOPPED) break;

                    String dummyUrl = "https://example.com/broken-" + i;
                    LinkResult result = new LinkResult(dummyUrl, "404 Not Found", "https://example.com/page" + i, "click here");

                    Platform.runLater(() -> {
                        allResults.add(result);
                        totalPageCount = (int) Math.ceil((double) allResults.size() / ROWS_PER_PAGE);
                        updateCurrentPage(currentPage - 1);
                        updateCustomPagination();

                        totalPages++;
                        totalLinks += 5;
                        updateCounts();
                        statusLabel.setText("Checking page " + totalPages);
                    });
                    Thread.sleep(100);
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
        showAlert("Export", "Export feature is not implemented yet.");
    }

    private void updateCustomPagination() {
        customPagination.getChildren().clear();

        HBox buttonBox = new HBox(5);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // Tombol Prev (⯇)
        Button prevButton = new Button("⯇");
        stylePageButton(prevButton);
        prevButton.setDisable(currentPage == 1);
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                updateCurrentPage(currentPage - 1);
                updateCustomPagination();
            }
        });

        // Tombol Next (⯈)
        Button nextButton = new Button("⯈");
        stylePageButton(nextButton);
        nextButton.setDisable(currentPage == totalPageCount || totalPageCount == 0);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPageCount) {
                currentPage++;
                updateCurrentPage(currentPage - 1);
                updateCustomPagination();
            }
        });

        buttonBox.getChildren().add(prevButton);

        // Hitung range halaman yang akan ditampilkan
        int startPage, endPage;
        if (totalPageCount <= MAX_PAGE_BUTTONS) {
            startPage = 1;
            endPage = totalPageCount;
        } else {
            int half = MAX_PAGE_BUTTONS / 2;
            if (currentPage <= half + 1) {
                startPage = 1;
                endPage = MAX_PAGE_BUTTONS;
            } else if (currentPage >= totalPageCount - half) {
                startPage = totalPageCount - MAX_PAGE_BUTTONS + 1;
                endPage = totalPageCount;
            } else {
                startPage = currentPage - half;
                endPage = currentPage + half;
            }
        }

        for (int i = startPage; i <= endPage; i++) {
            Button btn = new Button(String.valueOf(i));
            stylePageButton(btn);
            if (i == currentPage) btn.getStyleClass().add("current-page");
            final int pageIndex = i;
            btn.setOnAction(e -> {
                currentPage = pageIndex;
                updateCurrentPage(pageIndex - 1);
                updateCustomPagination();
            });
            buttonBox.getChildren().add(btn);
        }

        buttonBox.getChildren().add(nextButton);

        // Label halaman di bawah tombol
        Label pageInfo = new Label("Halaman " + currentPage + " / " + (totalPageCount == 0 ? 1 : totalPageCount));
        pageInfo.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");
        VBox.setMargin(pageInfo, new javafx.geometry.Insets(4, 0, 0, 0));
        pageInfo.setMaxWidth(Double.MAX_VALUE);
        pageInfo.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        customPagination.getChildren().addAll(buttonBox, pageInfo);
    }

    private void stylePageButton(Button btn) {
        btn.getStyleClass().add("page-button");
        btn.setMinWidth(PAGE_BUTTON_WIDTH);
        btn.setPrefWidth(PAGE_BUTTON_WIDTH);
        btn.setMaxWidth(PAGE_BUTTON_WIDTH);
    }

    private void updateCurrentPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, allResults.size());
        currentPageResults.setAll(allResults.subList(fromIndex, toIndex));
    }

    private void setStatus(CheckStatus status) {
        this.currentStatus = status;
        switch (status) {
            case IDLE -> {
                statusLabel.setText("Not started");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(true);
            }
            case CHECKING -> {
                statusLabel.setText("Checking...");
                checkButton.setDisable(true);
                stopButton.setDisable(false);
                exportButton.setDisable(true);
            }
            case COMPLETED -> {
                statusLabel.setText("Completed ✔");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(false);
            }
            case STOPPED -> {
                statusLabel.setText("Stopped ⏹");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(false);
            }
            case ERROR -> {
                statusLabel.setText("Error ❌");
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
                allResults.stream().filter(l -> l.getStatus().startsWith("4") || l.getStatus().startsWith("5")).count()
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
