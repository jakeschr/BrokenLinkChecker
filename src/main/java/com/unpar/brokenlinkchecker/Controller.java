package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.ExecutionStatus;
import com.unpar.brokenlinkchecker.model.LinkResult;

import com.unpar.brokenlinkchecker.service.LinkCheckerService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class Controller {
    // ============================== Komponen GUI =================================== //
    @FXML private TextField urlField;                               // Input URL awal yang akan dicek pengguna
    @FXML private ComboBox<String> techChoiceBox;                   // Pilihan teknologi crawling: "Jsoup" atau "Playwright"
    @FXML private Button checkButton;                               // Tombol untuk memulai proses pemeriksaan tautan
    @FXML private Button stopButton;                                // Tombol untuk menghentikan proses pemeriksaan yang sedang berjalan
    @FXML private Button exportButton;                              // Tombol untuk mengekspor hasil pemeriksaan (belum diimplementasikan)
    @FXML private Label statusLabel;                                // Label status yang menampilkan keadaan proses (e.g. "Checking...", "Completed")
    @FXML private Label pageCountLabel;                             // Label jumlah total halaman yang berhasil dicrawl
    @FXML private Label linkCountLabel;                             // Label jumlah total tautan (baik halaman maupun umum) yang ditemukan
    @FXML private Label brokenCountLabel;                           // Label jumlah total tautan rusak
    @FXML private VBox customPagination;                            // Container VBox untuk pagination kustom di bawah tabel hasil
    @FXML private TableView<LinkResult> resultTable;                // Tabel utama untuk menampilkan hasil pemeriksaan semua tautan
    @FXML private TableColumn<LinkResult, Number> colNumber;        // Kolom nomor urut (otomatis berdasarkan pagination)
    @FXML private TableColumn<LinkResult, String> colBrokenLink;    // Kolom berisi tautan rusak
    @FXML private TableColumn<LinkResult, String> colStatus;        // Kolom berisi status dari URL (kode HTTP atau error lainnya)
    @FXML private TableColumn<LinkResult, String> colSourcePage;    // Kolom berisi url halaman sumber tautan rusak ditemukan
    @FXML private TableColumn<LinkResult, String> colAnchorText;    // Kolom berisi teks anchor (teks yang diklik oleh pengguna)


    // ============================== Variabel Internal =================================== //
    // Service utama untuk menjalankan crawling dan pengecekan link
    private LinkCheckerService linkCheckerService;

    // Menyimpan seluruh hasil pemeriksaan tautan (tanpa filter pagination)
    private final ObservableList<LinkResult> allResults = FXCollections.observableArrayList();

    // Menyimpan hasil yang ditampilkan pada halaman saat ini (digunakan oleh TableView untuk pagination)
    private final ObservableList<LinkResult> currentPageResults = FXCollections.observableArrayList();

    private ExecutionStatus currentStatus = ExecutionStatus.IDLE;       // Status proses aplikasi saat ini (IDLE, CHECKING, COMPLETED, dll.)
    private int totalLinks = 0;                                         // Jumlah total tautan yang ditemukan (baik internal maupun umum)
    private int totalPages = 0;                                         // Jumlah total halaman internal yang berhasil dicrawl
    private int currentPage = 1;                                        // Halaman saat ini dalam tampilan tabel (mulai dari 1)
    private int totalPageCount = 0;                                     // Jumlah total halaman berdasarkan ukuran data dan ROWS_PER_PAGE
    private static final int MAX_PAGE_BUTTONS = 5;                      // Jumlah maksimal tombol halaman yang ditampilkan pada pagination kustom
    private static final int ROWS_PER_PAGE = 10;                        // Jumlah baris hasil yang ditampilkan per halaman tabel
    private static final double PAGE_BUTTON_WIDTH = 40;                 // Lebar tetap untuk setiap tombol pagination (dalam piksel)


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
        setStatus(ExecutionStatus.IDLE);
    }

    @FXML
    public void onCheckClick() {

    }

    @FXML
    public void onStopClick() {
        setStatus(ExecutionStatus.STOPPED);
    }

    @FXML
    public void onExportClick() {
        showAlert("Export", "Export feature is not implemented yet.");
    }

    private void setStatus(ExecutionStatus status) {
        this.currentStatus = status;
        switch (status) {
            case IDLE:
                statusLabel.setText("Not started");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(true);
                break;
            case CHECKING:
                statusLabel.setText("Checking...");
                checkButton.setDisable(true);
                stopButton.setDisable(false);
                exportButton.setDisable(true);
                break;
            case COMPLETED:
                statusLabel.setText("Completed ✔");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(false);
                break;
            case STOPPED:
                statusLabel.setText("Stopped ⏹");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(false);
                break;
            case ERROR:
                statusLabel.setText("Error ❌");
                checkButton.setDisable(false);
                stopButton.setDisable(true);
                exportButton.setDisable(true);
                break;
            default:
                showAlert("Execution Status", "Unsupported execution status: " + status + ".");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateCounts() {
        pageCountLabel.setText(String.valueOf(totalPages));
        linkCountLabel.setText(String.valueOf(totalLinks));
        brokenCountLabel.setText(String.valueOf(
                allResults.stream().filter(l -> l.getStatus().startsWith("4") || l.getStatus().startsWith("5")).count()
        ));
    }
    
    private void updatePagination() {
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
                updatePagination();
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
                updatePagination();
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
                updatePagination();
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

    private void updateCurrentPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, allResults.size());
        currentPageResults.setAll(allResults.subList(fromIndex, toIndex));
    }

    private void stylePageButton(Button btn) {
        btn.getStyleClass().add("page-button");
        btn.setMinWidth(PAGE_BUTTON_WIDTH);
        btn.setPrefWidth(PAGE_BUTTON_WIDTH);
        btn.setMaxWidth(PAGE_BUTTON_WIDTH);
    }
}
