module com.securebox {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.eclipse.paho.client.mqttv3;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    opens com.securebox to javafx.fxml;
    exports com.securebox;
}
