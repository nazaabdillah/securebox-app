package com.securebox;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import com.securebox.models.EkspedisiServer;
import com.securebox.models.Paket;
import com.securebox.models.Pengguna;
import com.securebox.models.SecureBoxLocker;
import com.securebox.models.DatabaseHelper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class App extends Application {

    private EkspedisiServer server = new EkspedisiServer();
    private SecureBoxLocker loker = new SecureBoxLocker("LOKER-001");
    private Pengguna user = new Pengguna("U01", "QORI NAZA", 200000.0);
    private Paket paketAktif = null;

    // --- PALET WARNA PROFESSIONAL ---
    private final String OFF_WHITE = "#F7F7F7"; 
    private final String KLIEN_BLUE = "#0047AB"; 
    private final String PURE_BLACK = "#000000"; 
    private final String NEON_GREEN = "#00FF7F"; 
    private final String NEON_RED = "#FF3131";   

    // --- NEUBRUTALISM REFINED (COMPACT) ---
    private final String FONT_FAMILY = "-fx-font-family: 'Arial Black'; ";
    
    private final String NEU_CARD = FONT_FAMILY +
        "-fx-background-color: #FFFFFF; " +
        "-fx-border-color: " + PURE_BLACK + "; " +
        "-fx-border-width: 3px; " +
        "-fx-effect: dropshadow(one-pass-box, " + PURE_BLACK + ", 0, 0, 6, 6);";

    private final String NEU_INPUT = FONT_FAMILY +
        "-fx-background-color: #FFFFFF; " +
        "-fx-text-fill: " + PURE_BLACK + "; " +
        "-fx-prompt-text-fill: #A0A0A0; " +
        "-fx-border-color: " + PURE_BLACK + "; " +
        "-fx-border-width: 3px; " +
        "-fx-padding: 10px; " +
        "-fx-font-size: 13px; " +
        "-fx-font-weight: bold;";

    private final String NEU_BTN_PRIMARY = FONT_FAMILY +
        "-fx-background-color: " + KLIEN_BLUE + "; " +
        "-fx-text-fill: white; " +
        "-fx-font-weight: 900; " +
        "-fx-border-color: " + PURE_BLACK + "; " +
        "-fx-border-width: 3px; " +
        "-fx-effect: dropshadow(one-pass-box, " + PURE_BLACK + ", 0, 0, 4, 4); " +
        "-fx-cursor: hand; " +
        "-fx-padding: 10px;";

    private final String NEU_BTN_SUCCESS = FONT_FAMILY +
        "-fx-background-color: " + NEON_GREEN + "; " +
        "-fx-text-fill: " + PURE_BLACK + "; " +
        "-fx-font-weight: 900; " +
        "-fx-border-color: " + PURE_BLACK + "; " +
        "-fx-border-width: 3px; " +
        "-fx-effect: dropshadow(one-pass-box, " + PURE_BLACK + ", 0, 0, 4, 4); " +
        "-fx-cursor: hand; " +
        "-fx-padding: 10px;";

    @Override
    public void start(Stage stage) {
        DatabaseHelper.inisialisasiDatabase();

        // --- HEADER (DIPERKECIL) ---
        VBox titleBox = new VBox(2);
        titleBox.setAlignment(Pos.CENTER);
        Label title = new Label("SECURE-BOX");
        title.setStyle(FONT_FAMILY + "-fx-text-fill: " + KLIEN_BLUE + "; -fx-font-size: 32px; -fx-font-weight: 900; -fx-letter-spacing: 2px;");
        Label subtitle = new Label("IOT LOGISTICS PROTOCOL");
        subtitle.setStyle(FONT_FAMILY + "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: " + PURE_BLACK + "; -fx-padding: 2px 8px;");
        titleBox.getChildren().addAll(title, subtitle);

        // --- TAB LOKER ---
        VBox viewLoker = new VBox(15); // Dikurangi dari 25
        
        VBox cardSaldo = new VBox(2);
        cardSaldo.setStyle(NEU_CARD);
        cardSaldo.setPadding(new Insets(10, 15, 10, 15));
        Label lblUser = new Label("OPERATOR: " + user.getNama().toUpperCase());
        lblUser.setStyle(FONT_FAMILY + "-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: " + KLIEN_BLUE + ";");
        Label lblSaldo = new Label("Rp " + String.format("%,.0f", user.getSaldoEWallet()));
        lblSaldo.setStyle(FONT_FAMILY + "-fx-font-size: 28px; -fx-font-weight: 900;");
        cardSaldo.getChildren().addAll(lblUser, lblSaldo);

        VBox cardAction = new VBox(12);
        cardAction.setStyle(NEU_CARD);
        cardAction.setPadding(new Insets(15, 20, 15, 20));
        cardAction.setAlignment(Pos.CENTER);

        TextField inputResi = new TextField();
        inputResi.setPromptText("INPUT NO. RESI");
        inputResi.setStyle(NEU_INPUT);

        Button btnVerify = new Button("VERIFY PROTOCOL");
        btnVerify.setStyle(NEU_BTN_PRIMARY);
        btnVerify.setMaxWidth(Double.MAX_VALUE);

        // Layar LCD UI (Dimaksimalkan tingginya agar tidak makan tempat)
        StackPane layarLCD = new StackPane();
        layarLCD.setMinHeight(180); // Dikurangi dari 220
        layarLCD.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: " + PURE_BLACK + "; -fx-border-width: 3px;");
        
        Label statusMsg = new Label("AWAITING INPUT...");
        statusMsg.setStyle(FONT_FAMILY + "-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: #A0A0A0;");
        ImageView qrView = new ImageView();
        qrView.setFitHeight(150);
        qrView.setPreserveRatio(true);
        layarLCD.getChildren().addAll(statusMsg, qrView);

        Button btnPay = new Button("AUTHORIZE & PAY");
        btnPay.setDisable(true);
        btnPay.setStyle(NEU_BTN_SUCCESS);
        btnPay.setMaxWidth(Double.MAX_VALUE);

        btnVerify.setOnAction(e -> {
            paketAktif = server.verifikasiResi(inputResi.getText());
            if (paketAktif != null && !paketAktif.getStatusPembayaran().equals("Lunas")) {
                qrView.setImage(generateQR(loker.generateQRPembayaran(paketAktif)));
                statusMsg.setVisible(false);
                btnPay.setDisable(false);
            } else if (paketAktif != null && paketAktif.getStatusPembayaran().equals("Lunas")) {
                statusMsg.setText("STATUS: PAID"); 
                statusMsg.setStyle(FONT_FAMILY + "-fx-text-fill: " + KLIEN_BLUE + "; -fx-font-weight: 900;");
                statusMsg.setVisible(true); qrView.setImage(null); btnPay.setDisable(true);
            } else {
                statusMsg.setText("ERR: INVALID"); 
                statusMsg.setStyle(FONT_FAMILY + "-fx-text-fill: " + NEON_RED + "; -fx-font-weight: 900;");
                statusMsg.setVisible(true); qrView.setImage(null); btnPay.setDisable(true);
            }
        });

        btnPay.setOnAction(e -> {
            if (user.bayarCOD(paketAktif.getNominalCOD())) {
                server.updateStatusLunas(paketAktif.getNoResi());
                loker.bukaPintu(); 
                lblSaldo.setText("Rp " + String.format("%,.0f", user.getSaldoEWallet()));
                statusMsg.setText("SUCCESS - OPEN"); 
                statusMsg.setStyle(FONT_FAMILY + "-fx-text-fill: " + KLIEN_BLUE + "; -fx-font-weight: 900;");
                statusMsg.setVisible(true); qrView.setImage(null);
                btnPay.setDisable(true); inputResi.clear();
            }
        });

        cardAction.getChildren().addAll(inputResi, btnVerify, layarLCD, btnPay);
        viewLoker.getChildren().addAll(cardSaldo, cardAction);

        // --- TAB ADMIN ---
        VBox viewAdmin = new VBox(15);
        VBox cardAdmin = new VBox(15);
        cardAdmin.setStyle(NEU_CARD);
        cardAdmin.setPadding(new Insets(20));
        cardAdmin.setAlignment(Pos.CENTER);

        Label lblAdmin = new Label("DATABASE SYSTEM");
        lblAdmin.setStyle(FONT_FAMILY + "-fx-font-weight: 900; -fx-font-size: 18px;");

        TextField resiIn = new TextField(); 
        resiIn.setPromptText("NEW RESI");
        resiIn.setStyle(NEU_INPUT);
        
        TextField hargaIn = new TextField(); 
        hargaIn.setPromptText("AMOUNT");
        hargaIn.setStyle(NEU_INPUT);

        Button btnSave = new Button("COMMIT TO DATABASE");
        btnSave.setStyle(NEU_BTN_PRIMARY);
        btnSave.setMaxWidth(Double.MAX_VALUE);

        Label lblNotif = new Label("");
        lblNotif.setStyle(FONT_FAMILY + "-fx-font-weight: 900; -fx-font-size: 12px;");

        btnSave.setOnAction(e -> {
            try {
                double harga = Double.parseDouble(hargaIn.getText());
                if(server.tambahPaketBaru(resiIn.getText(), harga)) {
                    lblNotif.setText("SUCCESS: DATA STORED");
                    lblNotif.setStyle(FONT_FAMILY + "-fx-text-fill: " + KLIEN_BLUE + ";");
                    resiIn.clear(); hargaIn.clear();
                } else {
                    lblNotif.setText("ERR: DUPLICATE");
                    lblNotif.setStyle(FONT_FAMILY + "-fx-text-fill: " + NEON_RED + ";");
                }
            } catch (Exception ex) {
                lblNotif.setText("ERR: NUMERIC ONLY");
                lblNotif.setStyle(FONT_FAMILY + "-fx-text-fill: " + NEON_RED + ";");
            }
        });

        cardAdmin.getChildren().addAll(lblAdmin, resiIn, hargaIn, btnSave, lblNotif);
        viewAdmin.getChildren().add(cardAdmin);

        // --- NAVIGASI ---
        HBox navBar = new HBox(10);
        navBar.setAlignment(Pos.CENTER);
        
        String tStyle = FONT_FAMILY + "-fx-background-color: white; -fx-border-color: black; -fx-border-width: 2px; -fx-cursor: hand; -fx-padding: 8px 15px; -fx-font-size: 12px; -fx-font-weight: 900;";
        Button navLoker = new Button("TERMINAL");
        Button navAdmin = new Button("ADMIN");
        navLoker.setStyle(tStyle + "-fx-background-color: " + KLIEN_BLUE + "; -fx-text-fill: white;");
        navAdmin.setStyle(tStyle);

        StackPane contentArea = new StackPane(viewLoker); 
        contentArea.setPadding(new Insets(10, 0, 0, 0));

        navLoker.setOnAction(e -> {
            navLoker.setStyle(tStyle + "-fx-background-color: " + KLIEN_BLUE + "; -fx-text-fill: white;");
            navAdmin.setStyle(tStyle);
            contentArea.getChildren().setAll(viewLoker);
        });

        navAdmin.setOnAction(e -> {
            navAdmin.setStyle(tStyle + "-fx-background-color: " + KLIEN_BLUE + "; -fx-text-fill: white;");
            navLoker.setStyle(tStyle);
            contentArea.getChildren().setAll(viewAdmin);
        });

        navBar.getChildren().addAll(navLoker, navAdmin);

        // --- ROOT LAYOUT ---
        VBox mainLayout = new VBox(15, titleBox, navBar, contentArea);
        mainLayout.setPadding(new Insets(20)); // Dikurangi dari 30
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setStyle("-fx-background-color: " + OFF_WHITE + ";");

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true); 
        scrollPane.setStyle("-fx-background: " + OFF_WHITE + "; -fx-background-color: " + OFF_WHITE + "; -fx-border-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Scene scene = new Scene(scrollPane, 420, 680); // Dikurangi ke 680 agar pas di layar laptop
        stage.setScene(scene);
        stage.setTitle("SECURE-BOX v2.1");
        stage.setResizable(false);
        stage.show();
    }

    private WritableImage generateQR(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200);
            WritableImage img = new WritableImage(200, 200);
            PixelWriter pw = img.getPixelWriter();
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    pw.setColor(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return img;
        } catch (Exception e) { return null; }
    }

    public static void main(String[] args) { launch(); }
}