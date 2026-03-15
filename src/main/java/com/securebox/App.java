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

// Import Model & IoT
import com.securebox.models.EkspedisiServer;
import com.securebox.models.Paket;
import com.securebox.models.Pengguna;
import com.securebox.models.SecureBoxLocker;
import com.securebox.models.DatabaseHelper;

// Import ZXing untuk QR Code
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class App extends Application {

    private EkspedisiServer server = new EkspedisiServer();
    private SecureBoxLocker loker = new SecureBoxLocker("LOKER-001");
    private Pengguna user = new Pengguna("U01", "Qori Naza", 200000.0);
    private Paket paketAktif = null;

    @Override
    public void start(Stage stage) {
        // 1. Inisialisasi Database
        DatabaseHelper.inisialisasiDatabase();

        // 2. Main Layout dengan TabPane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // ==========================================
        //        TAB 1 : HALAMAN LOKER (USER/KURIR)
        // ==========================================
        Tab tabLoker = new Tab("📱 Layar Loker");
        
        // Card Saldo
        VBox cardSaldo = new VBox(5);
        cardSaldo.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-padding: 15px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        Label lblUser = new Label("Hai, " + user.getNama() + " 👋");
        Label lblSaldo = new Label("Rp " + String.format("%,.0f", user.getSaldoEWallet()));
        lblSaldo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        cardSaldo.getChildren().addAll(lblUser, new Label("Saldo e-Wallet"), lblSaldo);

        // Area Interaction
        VBox cardAction = new VBox(15);
        cardAction.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-padding: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        cardAction.setAlignment(Pos.CENTER);

        TextField inputResi = new TextField();
        inputResi.setPromptText("Kurir: Masukkan No. Resi");
        inputResi.setStyle("-fx-background-radius: 8px; -fx-padding: 10px;");

        Button btnVerify = new Button("Verifikasi & Munculkan QR");
        btnVerify.setStyle("-fx-background-color: #00AA5B; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 10px 20px; -fx-cursor: hand;");

        // Layar LCD Loker (Tempat QR Code)
        StackPane layarLCD = new StackPane();
        layarLCD.setMinHeight(220);
        layarLCD.setStyle("-fx-background-color: #F3F4F5; -fx-background-radius: 8px; -fx-border-color: #DDDDDD; -fx-border-radius: 8px;");
        
        Label placeholderText = new Label("Menunggu Input Resi...");
        ImageView qrImageView = new ImageView();
        layarLCD.getChildren().addAll(placeholderText, qrImageView);

        Button btnPay = new Button("Scan QR & Bayar COD");
        btnPay.setDisable(true);
        btnPay.setStyle("-fx-background-color: #00AA5B; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 12px 30px;");

        // Logika Tombol Verifikasi
        btnVerify.setOnAction(e -> {
            paketAktif = server.verifikasiResi(inputResi.getText());
            if (paketAktif != null && !paketAktif.getStatusPembayaran().equals("Lunas")) {
                // Generate QR Code Asli
                String tokenData = loker.generateQRPembayaran(paketAktif);
                qrImageView.setImage(generateQR(tokenData));
                placeholderText.setVisible(false);
                btnPay.setDisable(false);
                System.out.println(">> [SISTEM] QR Code Generated: " + tokenData);
            } else if (paketAktif != null && paketAktif.getStatusPembayaran().equals("Lunas")) {
                placeholderText.setText("✅ PAKET SUDAH LUNAS");
                placeholderText.setVisible(true);
                qrImageView.setImage(null);
            } else {
                placeholderText.setText("❌ RESI TIDAK VALID");
                placeholderText.setVisible(true);
                qrImageView.setImage(null);
            }
        });

        // Logika Tombol Bayar (Trigger IoT & DB)
        btnPay.setOnAction(e -> {
            if (user.bayarCOD(paketAktif.getNominalCOD())) {
                server.updateStatusLunas(paketAktif.getNoResi());
                loker.bukaPintu(); // Memicu sinyal MQTT ke Wokwi
                
                lblSaldo.setText("Rp " + String.format("%,.0f", user.getSaldoEWallet()));
                placeholderText.setText("✅ PEMBAYARAN BERHASIL\nPintu Loker Terbuka!");
                placeholderText.setVisible(true);
                qrImageView.setImage(null);
                btnPay.setDisable(true);
                inputResi.clear();
            } else {
                placeholderText.setText("⚠️ SALDO TIDAK CUKUP");
                placeholderText.setVisible(true);
            }
        });

        cardAction.getChildren().addAll(inputResi, btnVerify, layarLCD, btnPay);
        VBox viewLoker = new VBox(20, cardSaldo, cardAction);
        viewLoker.setPadding(new Insets(20));
        viewLoker.setStyle("-fx-background-color: #F8F9FA;");
        tabLoker.setContent(viewLoker);

        // ==========================================
        //        TAB 2 : HALAMAN ADMIN (INPUT DB)
        // ==========================================
        Tab tabAdmin = new Tab("💻 Admin Server");
        VBox cardAdmin = new VBox(15);
        cardAdmin.setPadding(new Insets(20));
        cardAdmin.setAlignment(Pos.TOP_CENTER);

        Label lblAdmin = new Label("Input Paket Baru ke Sistem");
        lblAdmin.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        TextField resiIn = new TextField(); resiIn.setPromptText("No Resi");
        TextField hargaIn = new TextField(); hargaIn.setPromptText("Harga COD");
        Button btnSave = new Button("Simpan ke Database");
        btnSave.setStyle("-fx-background-color: #1A73E8; -fx-text-fill: white; -fx-font-weight: bold;");

        btnSave.setOnAction(e -> {
            try {
                double harga = Double.parseDouble(hargaIn.getText());
                if(server.tambahPaketBaru(resiIn.getText(), harga)) {
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Paket Berhasil Disimpan!");
                    a.show();
                    resiIn.clear(); hargaIn.clear();
                }
            } catch (Exception ex) {
                System.out.println("Input Salah!");
            }
        });

        cardAdmin.getChildren().addAll(lblAdmin, resiIn, hargaIn, btnSave);
        tabAdmin.setContent(cardAdmin);

        // Header & Assemble
        tabPane.getTabs().addAll(tabLoker, tabAdmin);
        HBox header = new HBox(new Label("SECURE-BOX IOT"));
        header.setStyle("-fx-background-color: #00AA5B; -fx-padding: 15px;");
        header.setAlignment(Pos.CENTER_LEFT);
        ((Label)header.getChildren().get(0)).setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 18px;");

        VBox root = new VBox(header, tabPane);
        stage.setScene(new Scene(root, 380, 650));
        stage.setTitle("SecureBox - Prototype");
        stage.setResizable(false);
        stage.show();
    }

    // FUNGSI SAKTI: Mengubah Teks menjadi Gambar QR Code
    private WritableImage generateQR(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
            WritableImage image = new WritableImage(200, 200);
            PixelWriter pw = image.getPixelWriter();
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    pw.setColor(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}