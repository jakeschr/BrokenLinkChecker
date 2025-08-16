package com.unpar.brokenlinkchecker;

import com.unpar.brokenlinkchecker.model.CrawlResult;
import com.unpar.brokenlinkchecker.model.CrawledPage;
import com.unpar.brokenlinkchecker.model.ExecutionStatus;
import com.unpar.brokenlinkchecker.model.LinkResult;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Controller untuk UI BrokenLinkChecker (diselaraskan dengan FXML terbaru).
 * - ID kolom & tabel disesuaikan (colNumber1/2, colStatus1/2, dst.)
 * - Toggle: brokenLinkToggle & webpageToggle
 * - Streaming data ke dua tabel (broken links & webpages)
 * - Pagination untuk Broken Links Table
 */
public class Controller {

    // ===================== Komponen FXML =====================
    @FXML
    private TextField seedUrl;
    @FXML
    private ComboBox<String> algoChoiceBox;
    @FXML
    private Button checkButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button exportButton;

    @FXML
    private Label statusLabel;
    @FXML
    private Label pageCountLabel;
    @FXML
    private Label linkCountLabel;
    @FXML
    private Label brokenCountLabel;

    @FXML
    private VBox customPagination;

    // Broken Links Table (sesuai FXML terbaru)
    @FXML
    private TableView<LinkResult> brokenLinkTable;
    @FXML
    private TableColumn<LinkResult, Number> colNumber1;
    @FXML
    private TableColumn<LinkResult, String> colStatus1;
    @FXML
    private TableColumn<LinkResult, String> colBrokenLink1;
    @FXML
    private TableColumn<LinkResult, String> colAnchorText1;
    @FXML
    private TableColumn<LinkResult, String> colWebpageLink1;

    // Webpages Table (sesuai FXML terbaru)
    @FXML
    private TableView<CrawledPage> webpageTable;
    @FXML
    private TableColumn<CrawledPage, Number> colNumber2;
    @FXML
    private TableColumn<CrawledPage, String> colStatus2;
    @FXML
    private TableColumn<CrawledPage, String> colWebpageLink2;
    @FXML
    private TableColumn<CrawledPage, Number> colLinkCount2;
    @FXML
    private TableColumn<CrawledPage, String> colAccessTime2;

    // Toggle (sesuai FXML terbaru)
    @FXML
    private ToggleButton brokenLinkToggle;
    @FXML
    private ToggleButton webpageToggle;

    // ===================== Variabel Internal =====================
    private final ObservableList<LinkResult> allResults = FXCollections.observableArrayList();       // semua broken links
    private final ObservableList<LinkResult> currentPageResults = FXCollections.observableArrayList(); // halaman saat ini (pagination)

    private final ObservableList<CrawledPage> webpageList = FXCollections.observableArrayList();     // semua webpage same-host (sukses/gagal)

    private Service service;
    private ExecutionStatus currentStatus = ExecutionStatus.IDLE;
    private CrawlResult lastCrawlResult = null;

    private int currentPage = 1;
    private int totalPageCount = 0;
    private static final int ROWS_PER_PAGE = 5;
    private static final int MAX_PAGE_BUTTONS = 5;
    private static final double PAGE_BUTTON_WIDTH = 40;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ===================== Inisialisasi =====================
    @FXML
    public void initialize() {
        setupAlgoChoiceBox();
        setupBrokenLinkTableColumns();
        setupWebpageTableColumns();

        brokenLinkTable.setItems(currentPageResults);
        webpageTable.setItems(webpageList);

        setStatus(ExecutionStatus.IDLE);
        setupToggleView();
        updatePagination();
    }

    // ===================== Setup kolom tabel =====================
    private void setupBrokenLinkTableColumns() {
        // Lebar relatif (mengacu ke brokenLinkTable)
        colNumber1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.06)); // sedikit lebih lebar
        colStatus1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.16));
        colBrokenLink1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.36));
        colAnchorText1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.18));
        colWebpageLink1.prefWidthProperty().bind(brokenLinkTable.widthProperty().multiply(0.24));

        // Data binding
        colNumber1.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                (currentPage - 1) * ROWS_PER_PAGE + currentPageResults.indexOf(cell.getValue()) + 1
        ));
        colStatus1.setCellValueFactory(cell -> cell.getValue().statusProperty());          // gunakan HttpStatus.getReasonPhrase() di backend saat set
        colBrokenLink1.setCellValueFactory(cell -> cell.getValue().brokenLinkProperty());
        colAnchorText1.setCellValueFactory(cell -> cell.getValue().anchorTextProperty());
        colWebpageLink1.setCellValueFactory(cell -> cell.getValue().sourcePageProperty());
    }

    private void setupWebpageTableColumns() {
        // Lebar relatif (mengacu ke webpageTable)
        colNumber2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.06));
        colStatus2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.16));
        colWebpageLink2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.40));
        colLinkCount2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.14));
        colAccessTime2.prefWidthProperty().bind(webpageTable.widthProperty().multiply(0.24));

        // Data binding
        colNumber2.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                webpageList.indexOf(cell.getValue()) + 1
        ));
        colStatus2.setCellValueFactory(cell -> cell.getValue().statusProperty());          // gunakan HttpStatus.getReasonPhrase() di backend saat set
        colWebpageLink2.setCellValueFactory(cell -> cell.getValue().urlProperty());

        // IntegerProperty → asObject() bila model menyediakan IntegerProperty
        colLinkCount2.setCellValueFactory(cell -> cell.getValue().linkCountProperty());    // sesuaikan dengan model Anda

        // Format waktu dari LocalDateTime
        colAccessTime2.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> {
                    LocalDateTime t = cell.getValue().getAccessedTime();
                    return t != null ? TIME_FORMATTER.format(t) : "";
                },
                cell.getValue().accessTimeProperty()
        ));
    }

    // ===================== Algoritma & Toggle =====================
    private void setupAlgoChoiceBox() {
        algoChoiceBox.setItems(FXCollections.observableArrayList(
                "Breadth-First Search (BFS)",
                "Depth-First Search (DFS)"
        ));
        algoChoiceBox.getSelectionModel().select(0);
    }

    private void setupToggleView() {
        ToggleGroup toggleGroup = new ToggleGroup();
        brokenLinkToggle.setToggleGroup(toggleGroup);
        webpageToggle.setToggleGroup(toggleGroup);

        toggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == brokenLinkToggle) {
                showBrokenLinkTable();
            } else if (newToggle == webpageToggle) {
                showWebpageTable();
            }
        });

        // Tampilan awal
        brokenLinkToggle.setSelected(true);
        showBrokenLinkTable();
    }

    private void showBrokenLinkTable() {
        brokenLinkTable.setVisible(true);
        webpageTable.setVisible(false);
        updatePagination();
    }

    private void showWebpageTable() {
        brokenLinkTable.setVisible(false);
        webpageTable.setVisible(true);
    }

    // ===================== Actions =====================
    @FXML
    public void onCheckClick() {
        String url = seedUrl.getText();
        String algoLabel = algoChoiceBox.getValue();

        if (url == null || url.isBlank()) {
            showInfo("Input Error", "Seed URL tidak boleh kosong.");
            return;
        }
        if (algoLabel == null) {
            showInfo("Input Error", "Pilih Algorithm terlebih dahulu.");
            return;
        }

        String algorithm = algoLabel.contains("BFS") ? "BFS" : "DFS";

        // Auto-prepend skema jika kosong
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        resetData();
        setStatus(ExecutionStatus.CHECKING);

        service = new Service();

        // Streaming tautan rusak (real-time)
        service.setOnLinkResult(createBrokenLinkStreamConsumer());

        // Streaming halaman (Webpages Table)
        service.setOnCrawledPage(cp -> Platform.runLater(() -> webpageList.add(cp)));

        // Update metrik ringkasan
        service.setOnPageCountUpdate(this::updatePageCount);
        service.setOnTotalLinkUpdate(this::updateTotalLinkCount);
        service.setOnBrokenLinkUpdate(this::updateBrokenLinkCount);

        // Selesai & Error
        service.setOnComplete(this::onCrawlingComplete);
        service.setOnError(this::onCrawlingError);

        service.startCrawling(url, algorithm);
    }

    @FXML
    public void onStopClick() {
        if (service != null) {
            service.stop();
        }
        setStatus(ExecutionStatus.STOPPED);
    }

    @FXML
    public void onExportClick() {
        // TODO: implementasi ekspor CSV sesuai spesifikasi (unique/occurrence)
        showInfo("Export", "Fitur export belum diimplementasikan.");
    }

    // ===================== Callback streaming & status =====================
    private Consumer<LinkResult> createBrokenLinkStreamConsumer() {
        return result -> Platform.runLater(() -> {
            allResults.add(result);
            // refresh pagination hanya saat tab Broken Links terlihat, tapi aman untuk dipanggil kapan pun
            updatePagination();
        });
    }

    private void onCrawlingComplete(CrawlResult result) {
        Platform.runLater(() -> {
            this.lastCrawlResult = result;
            setStatus(ExecutionStatus.COMPLETED);
        });
    }

    private void onCrawlingError(String errorMessage) {
        Platform.runLater(() -> {
            setStatus(ExecutionStatus.ERROR);
            showInfo("Error", "Terjadi kesalahan saat crawling: " + errorMessage);
        });
    }

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

    // ===================== Reset & Metrik =====================
    private void resetData() {
        allResults.clear();
        currentPageResults.clear();
        webpageList.clear();

        currentPage = 1;
        totalPageCount = 0;
        pageCountLabel.setText("0");
        linkCountLabel.setText("0");
        brokenCountLabel.setText("0");
        updatePagination();

        lastCrawlResult = null;
    }

    private void updatePageCount(int count) {
        Platform.runLater(() -> pageCountLabel.setText(String.valueOf(count)));
    }

    private void updateTotalLinkCount(int count) {
        Platform.runLater(() -> linkCountLabel.setText(String.valueOf(count)));
    }

    private void updateBrokenLinkCount(int count) {
        Platform.runLater(() -> brokenCountLabel.setText(String.valueOf(count)));
    }

    // ===================== Pagination (Broken Links) =====================
    private void updatePagination() {
        customPagination.getChildren().clear();

        totalPageCount = (int) Math.ceil((double) allResults.size() / ROWS_PER_PAGE);
        if (totalPageCount == 0) totalPageCount = 1;
        if (currentPage > totalPageCount) currentPage = totalPageCount;

        HBox buttonBox = new HBox(5);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button prev = createPageButton("⯇", () -> goToPage(currentPage - 1), currentPage == 1);
        Button next = createPageButton("⯈", () -> goToPage(currentPage + 1), currentPage == totalPageCount);

        buttonBox.getChildren().add(prev);

        int start = Math.max(1, currentPage - MAX_PAGE_BUTTONS / 2);
        int end = Math.min(totalPageCount, start + MAX_PAGE_BUTTONS - 1);

        for (int i = start; i <= end; i++) {
            final int page = i;
            Button btn = createPageButton(String.valueOf(i), () -> goToPage(page), false);
            if (i == currentPage) btn.getStyleClass().add("current-page");
            buttonBox.getChildren().add(btn);
        }

        buttonBox.getChildren().add(next);
        customPagination.getChildren().addAll(buttonBox, new Label("Page " + currentPage + " / " + totalPageCount));

        updateCurrentPage(currentPage - 1); // index mulai 0
    }

    private Button createPageButton(String text, Runnable action, boolean disabled) {
        Button btn = new Button(text);
        btn.setMinWidth(PAGE_BUTTON_WIDTH);
        btn.setDisable(disabled);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void goToPage(int pageNumber) {
        if (pageNumber < 1 || pageNumber > totalPageCount) return;
        currentPage = pageNumber;
        updatePagination();
    }

    private void updateCurrentPage(int pageIndex) {
        int fromIndex = Math.max(0, pageIndex * ROWS_PER_PAGE);
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, allResults.size());
        if (fromIndex > toIndex) fromIndex = toIndex;
        currentPageResults.setAll(allResults.subList(fromIndex, toIndex));
    }

    // ===================== Alert util =====================
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
