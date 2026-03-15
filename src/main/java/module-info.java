module com.securebox {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.securebox to javafx.fxml;
    exports com.securebox;
}
