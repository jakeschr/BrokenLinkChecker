package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.CrawlResult;
import com.unpar.brokenlinkchecker.model.ExecutionStatus;
import com.unpar.brokenlinkchecker.model.LinkResult;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Controller utama untuk menangani interaksi UI aplikasi BrokenLinkChecker.
 * Bertanggung jawab untuk menginisialisasi tabel, menangani klik tombol, dan
 * menerima data dari Service secara streaming untuk ditampilkan ke tabel.
 */
public class Controller {


    // ===================== Komponen FXML (terhubung ke main.fxml) =====================
    @FXML
    private TextField seedUrl;                        // Input URL awal
    @FXML
    private ComboBox<String> techChoiceBox;           // Dropdown pilihan teknologi (Jsoup/Playwright)
    @FXML
    private ComboBox<String> algoChoiceBox;           // Dropdown pilihan algoritma (BFS/DFS)
    @FXML
    private Button checkButton;                       // Tombol untuk mulai pengecekan
    @FXML
    private Button stopButton;                        // Tombol untuk menghentikan pengecekan
    @FXML
    private Button exportButton;                      // Tombol untuk ekspor hasil (belum diimplementasikan)
    @FXML
    private Label statusLabel;                        // Label status (IDLE, CHECKING, dsb)
    @FXML
    private Label pageCountLabel;                     // Label total halaman yang dikunjungi
    @FXML
    private Label linkCountLabel;                     // Label total semua link yang ditemukan
    @FXML
    private Label brokenCountLabel;                   // Label total link rusak
    @FXML
    private VBox customPagination;                    // Komponen pagination khusus (bukan Pagination bawaan JavaFX)
    @FXML
    private TableView<LinkResult> resultTable;        // Tabel hasil link rusak
    @FXML
    private TableColumn<LinkResult, Number> colNumber;       // Kolom nomor
    @FXML
    private TableColumn<LinkResult, String> colBrokenLink;   // Kolom link rusak
    @FXML
    private TableColumn<LinkResult, String> colStatus;       // Kolom status HTTP
    @FXML
    private TableColumn<LinkResult, String> colSourcePage;   // Kolom halaman sumber
    @FXML
    private TableColumn<LinkResult, String> colAnchorText;   // Kolom teks anchor

    @FXML
    private ToggleButton brokenLinkToggle;
    @FXML
    private ToggleButton webPageToggle;

    // ===================== Variabel Internal =====================
    private final ObservableList<LinkResult> allResults = FXCollections.observableArrayList();       // Semua hasil link rusak
    private final ObservableList<LinkResult> currentPageResults = FXCollections.observableArrayList(); // Subset hasil untuk ditampilkan per halaman

    private Service service;                                  // Instance service backend
    private ExecutionStatus currentStatus = ExecutionStatus.IDLE;  // Status aplikasi saat ini
    private CrawlResult lastCrawlResult = null;               // Menyimpan hasil akhir crawling

    private int currentPage = 1;                              // Halaman saat ini dalam pagination
    private int totalPageCount = 0;                           // Total halaman dalam pagination
    private static final int ROWS_PER_PAGE = 25;              // Jumlah baris per halaman
    private static final int MAX_PAGE_BUTTONS = 5;            // Tombol navigasi maksimal yang ditampilkan
    private static final double PAGE_BUTTON_WIDTH = 40;       // Lebar tombol halaman

    /**
     * Dipanggil otomatis setelah FXML dimuat. Inisialisasi awal komponen tabel dan dropdown.
     */
    @FXML
    public void initialize() {
        setupTableColumns();      // Atur lebar kolom dan binding isi tabel
        setupTechChoiceBox();     // Atur pilihan dropdown teknologi
        resultTable.setItems(currentPageResults);  // Sambungkan data ke tabel
        setStatus(ExecutionStatus.IDLE);           // Set status awal
    }

    /**
     * Menentukan lebar dan isi kolom tabel hasil
     */
    private void setupTableColumns() {
        double totalRatio = 100.0;

        // Bind ukuran kolom agar proporsional dengan total lebar tabel
        colNumber.prefWidthProperty().bind(resultTable.widthProperty().multiply(3 / totalRatio));
        colStatus.prefWidthProperty().bind(resultTable.widthProperty().multiply(15 / totalRatio));
        colBrokenLink.prefWidthProperty().bind(resultTable.widthProperty().multiply(40 / totalRatio));
        colAnchorText.prefWidthProperty().bind(resultTable.widthProperty().multiply(17 / totalRatio));
        colSourcePage.prefWidthProperty().bind(resultTable.widthProperty().multiply(25 / totalRatio));

        // Mengatur isi tiap kolom
        colNumber.setCellValueFactory(cell -> Bindings.createIntegerBinding(() -> {
            int index = currentPageResults.indexOf(cell.getValue());
            return (currentPage - 1) * ROWS_PER_PAGE + index + 1;
        }));
        colBrokenLink.setCellValueFactory(cell -> cell.getValue().brokenLinkProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colSourcePage.setCellValueFactory(cell -> cell.getValue().sourcePageProperty());
        colAnchorText.setCellValueFactory(cell -> cell.getValue().anchorTextProperty());
    }

    /**
     * Inisialisasi pilihan teknologi crawling
     */
    private void setupTechChoiceBox() {
        techChoiceBox.setItems(FXCollections.observableArrayList("Jsoup", "Playwright"));
        techChoiceBox.getSelectionModel().selectFirst(); // Pilih default: Jsoup
    }

    /**
     * Event handler saat tombol "Check" diklik
     */
    @FXML
    public void onCheckClick() {
        String url = seedUrl.getText();        // Ambil input URL dari TextField
        String tech = techChoiceBox.getValue(); // Ambil pilihan teknologi

        if (url == null || url.isBlank()) {
            showAlert("Input Error", "Seed URL tidak boleh kosong.");
            return;
        }

        // Tambahkan http:// jika user tidak menulis protokol
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        resetData();                           // Bersihkan semua data sebelumnya
        setStatus(ExecutionStatus.CHECKING);   // Set status menjadi "Checking..."

        service = new Service();               // Buat instance baru dari service
        service.setOnLinkResult(createStreamingConsumer());         // Untuk menerima hasil link rusak saat crawling
        service.setOnComplete(this::onCrawlingComplete);            // Callback ketika crawling selesai
        service.setOnError(this::onCrawlingError);                  // Callback ketika error
        service.setOnPageCountUpdate(this::updatePageCount);       // Update label jumlah halaman
        service.setOnTotalLinkUpdate(this::updateTotalLinkCount);  // Update label jumlah tautan total
        service.setOnBrokenLinkUpdate(this::updateBrokenLinkCount);// Update label jumlah tautan rusak

        service.startCrawling(url, tech);      // Mulai crawling
    }

    /**
     * Event handler saat tombol "Stop" diklik
     */
    @FXML
    public void onStopClick() {
        if (service != null) {
            service.stop();   // Beri sinyal ke thread untuk berhenti
        }
        setStatus(ExecutionStatus.STOPPED); // Ubah status menjadi STOPPED
    }

    /**
     * Event handler tombol "Export"
     */
    @FXML
    public void onExportClick() {
        showAlert("Export", "Fitur export belum diimplementasikan.");
    }

    /**
     * Callback streaming per tautan rusak dari service
     */
    private Consumer<LinkResult> createStreamingConsumer() {
        return result -> Platform.runLater(() -> {
            allResults.add(result);  // Tambahkan ke daftar hasil
            updatePagination();      // Perbarui tampilan pagination
        });
    }

    /**
     * Callback saat crawling selesai
     */
    private void onCrawlingComplete(CrawlResult result) {
        Platform.runLater(() -> {
            this.lastCrawlResult = result;  // Simpan hasil akhir crawling
            setStatus(ExecutionStatus.COMPLETED); // Ganti status
        });
    }

    /**
     * Callback saat crawling gagal/error
     */
    private void onCrawlingError(String errorMessage) {
        Platform.runLater(() -> {
            setStatus(ExecutionStatus.ERROR);    // Set status error
            showAlert("Error", "Terjadi kesalahan saat crawling: " + errorMessage); // Tampilkan error
        });
    }

    /**
     * Atur label dan tombol berdasarkan status aplikasi
     */
    private void setStatus(ExecutionStatus status) {
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

    /**
     * Reset seluruh data dan tampilan sebelum proses baru
     */
    private void resetData() {
        allResults.clear();          // Kosongkan semua hasil
        currentPage = 1;
        totalPageCount = 0;
        pageCountLabel.setText("0");
        linkCountLabel.setText("0");
        brokenCountLabel.setText("0");
        updatePagination();          // Bersihkan pagination
        lastCrawlResult = null;      // Reset hasil crawling terakhir
    }

    /**
     * Update label total halaman secara real-time
     */
    private void updatePageCount(int count) {
        Platform.runLater(() -> pageCountLabel.setText(String.valueOf(count)));
    }

    /**
     * Update label total link secara real-time
     */
    private void updateTotalLinkCount(int count) {
        Platform.runLater(() -> linkCountLabel.setText(String.valueOf(count)));
    }

    /**
     * Update label total link rusak secara real-time
     */
    private void updateBrokenLinkCount(int count) {
        Platform.runLater(() -> brokenCountLabel.setText(String.valueOf(count)));
    }

    /**
     * Buat dan atur ulang tombol pagination
     */
    private void updatePagination() {
        customPagination.getChildren().clear(); // Bersihkan tombol sebelumnya

        totalPageCount = (int) Math.ceil((double) allResults.size() / ROWS_PER_PAGE);
        if (totalPageCount == 0) totalPageCount = 1;

        HBox buttonBox = new HBox(5);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button prev = createPageButton("⯇", () -> goToPage(currentPage - 1), currentPage == 1);
        Button next = createPageButton("⯈", () -> goToPage(currentPage + 1), currentPage == totalPageCount);

        buttonBox.getChildren().add(prev);

        int start = Math.max(1, currentPage - MAX_PAGE_BUTTONS / 2);
        int end = Math.min(totalPageCount, start + MAX_PAGE_BUTTONS - 1);

        for (int i = start; i <= end; i++) {
            int page = i;
            Button btn = createPageButton(String.valueOf(i), () -> goToPage(page), false);
            if (i == currentPage) btn.getStyleClass().add("current-page");
            buttonBox.getChildren().add(btn);
        }

        buttonBox.getChildren().add(next);
        customPagination.getChildren().addAll(buttonBox, new Label("Halaman " + currentPage + " / " + totalPageCount));

        updateCurrentPage(currentPage - 1); // index dari 0
    }

    /**
     * Buat tombol halaman dengan aksi tertentu
     */
    private Button createPageButton(String text, Runnable action, boolean disabled) {
        Button btn = new Button(text);
        btn.setMinWidth(PAGE_BUTTON_WIDTH);
        btn.setDisable(disabled);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    /**
     * Arahkan ke halaman tertentu
     */
    private void goToPage(int pageNumber) {
        if (pageNumber < 1 || pageNumber > totalPageCount) return;
        currentPage = pageNumber;
        updatePagination();
    }

    /**
     * Tampilkan subset hasil sesuai halaman yang aktif
     */
    private void updateCurrentPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, allResults.size());
        currentPageResults.setAll(allResults.subList(fromIndex, toIndex));
    }

    /**
     * Tampilkan alert popup informasi
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
