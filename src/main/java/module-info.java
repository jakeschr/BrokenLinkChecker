module com.unpar.brokenlinkchecker {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;

    // Modul tambahan dari pustaka pihak ketiga
    requires org.jsoup;
    requires java.net.http;
//    requires com.microsoft.playwright;

    opens com.unpar.brokenlinkchecker to javafx.fxml;
    exports com.unpar.brokenlinkchecker;
}