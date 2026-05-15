package com.securebox;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.securebox.models.EkspedisiServer;
import com.securebox.models.Paket;
import com.securebox.models.Pengguna;
import com.securebox.models.SecureBoxLocker;
import com.securebox.models.DatabaseHelper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class App extends Application {

    // ── Model ────────────────────────────────────────────────────────────────
    private EkspedisiServer server     = new EkspedisiServer();
    private SecureBoxLocker loker      = new SecureBoxLocker("LOKER-001");
    private Pengguna        user       = new Pengguna("U01", "QORI NAZA", 200000.0);
    private Paket           paketAktif = null;

    // ── iOS Color Palette ────────────────────────────────────────────────────
    private static final String BG        = "#F2F2F7";
    private static final String BG_CARD   = "#FFFFFF";
    private static final String BLUE      = "#007AFF";
    private static final String GREEN     = "#34C759";
    private static final String RED       = "#FF3B30";
    private static final String ORANGE    = "#FF9500";
    private static final String PURPLE    = "#AF52DE";
    private static final String TEAL      = "#5AC8FA";
    private static final String LBL_PRI   = "#1C1C1E";
    private static final String LBL_SEC   = "#8E8E93";
    private static final String LBL_TER   = "#C7C7CC";
    private static final String SEPARATOR = "#E5E5EA";

    // ── Font ─────────────────────────────────────────────────────────────────
    private static final String SF = "-fx-font-family: 'SF Pro Display', 'Segoe UI', 'Ubuntu', sans-serif; ";

    // ── UI refs ──────────────────────────────────────────────────────────────
    private Label     lblSaldo;
    private Label     statusMsg;
    private ImageView qrView;
    private Button    btnPay;
    private Label     footerStatus;

    @Override
    public void start(Stage stage) {
        DatabaseHelper.inisialisasiDatabase();

        VBox viewTerminal = buildTerminalView();
        VBox viewAdmin    = buildAdminView();

        // ── iOS Tab Bar ───────────────────────────────────────────────────────
        VBox tabTerminal = makeTab("📦", "Terminal");
        VBox tabAdmin    = makeTab("🗄", "Admin");
        tabTerminal.setStyle(tabActiveStyle(BLUE));
        tabAdmin.setStyle(tabInactiveStyle());

        HBox.setHgrow(tabTerminal, Priority.ALWAYS);
        HBox.setHgrow(tabAdmin,    Priority.ALWAYS);
        tabTerminal.setMaxWidth(Double.MAX_VALUE);
        tabAdmin.setMaxWidth(Double.MAX_VALUE);

        HBox tabBar = new HBox(tabTerminal, tabAdmin);
        tabBar.setStyle(
            "-fx-background-color: " + BG_CARD + "; " +
            "-fx-border-color: " + SEPARATOR + "; " +
            "-fx-border-width: 0.5px 0 0 0;"
        );

        StackPane contentArea = new StackPane(viewTerminal);
        contentArea.setStyle("-fx-background-color: " + BG + ";");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        tabTerminal.setOnMouseClicked(e -> {
            tabTerminal.setStyle(tabActiveStyle(BLUE));
            tabAdmin.setStyle(tabInactiveStyle());
            contentArea.getChildren().setAll(viewTerminal);
        });
        tabAdmin.setOnMouseClicked(e -> {
            tabAdmin.setStyle(tabActiveStyle(PURPLE));
            tabTerminal.setStyle(tabInactiveStyle());
            contentArea.getChildren().setAll(viewAdmin);
        });

        // ── Root ──────────────────────────────────────────────────────────────
        VBox root = new VBox();
        root.setStyle("-fx-background-color: " + BG + ";");
        root.getChildren().addAll(buildHeader(), contentArea, tabBar);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        Scene scene = new Scene(root, 390, 760);
        stage.setScene(scene);
        stage.setTitle("SecureBox");
        stage.setResizable(false);
        stage.show();

        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), root);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    // ── HEADER ───────────────────────────────────────────────────────────────
    private VBox buildHeader() {
        // Online pill
        HBox pill = new HBox(5);
        pill.setAlignment(Pos.CENTER);
        pill.setStyle(
            "-fx-background-color: " + GREEN + "22; " +
            "-fx-border-color: " + GREEN + "55; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 20px; " +
            "-fx-background-radius: 20px; " +
            "-fx-padding: 3px 10px;"
        );
        Label pillDot = new Label("●");
        pillDot.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 8px;");
        Label pillTxt = new Label("System Active");
        pillTxt.setStyle(SF + "-fx-text-fill: " + GREEN + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        FadeTransition pulse = new FadeTransition(Duration.millis(1800), pillDot);
        pulse.setFromValue(1.0); pulse.setToValue(0.25);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        pill.getChildren().addAll(pillDot, pillTxt);

        Label title = new Label("SecureBox");
        title.setStyle(SF +
            "-fx-text-fill: " + LBL_PRI + "; " +
            "-fx-font-size: 30px; " +
            "-fx-font-weight: bold;"
        );

        Label subtitle = new Label("IoT Smart Locker System");
        subtitle.setStyle(SF + "-fx-text-fill: " + LBL_SEC + "; -fx-font-size: 13px;");

        VBox headerContent = new VBox(6, pill, title, subtitle);
        headerContent.setPadding(new Insets(54, 20, 18, 20));
        headerContent.setStyle("-fx-background-color: " + BG_CARD + ";");

        Rectangle sep = new Rectangle(390, 0.5);
        sep.setFill(Color.web(SEPARATOR));

        VBox header = new VBox(headerContent, sep);
        header.setStyle("-fx-background-color: " + BG_CARD + ";");
        return header;
    }

    // ── TERMINAL VIEW ─────────────────────────────────────────────────────────
    private VBox buildTerminalView() {
        VBox outer = new VBox();
        outer.setStyle("-fx-background-color: " + BG + ";");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + "; -fx-border-color: transparent;");
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 16, 32, 16));
        content.setStyle("-fx-background-color: " + BG + ";");

        // Wallet card
        content.getChildren().add(buildWalletCard());

        // Verify card
        VBox verifyCard = iosCard();
        Label vHeader = sectionTitle("Package Tracking");
        Label vSub    = sectionSub("Enter resi number to verify package");

        TextField inputResi = new TextField();
        inputResi.setPromptText("e.g. JNE-2024-00123");
        inputResi.setStyle(iosInputStyle());

        Button btnVerify = iosButton("Verify Package", BLUE);

        statusMsg = new Label("Awaiting package number...");
        statusMsg.setStyle(SF + "-fx-text-fill: " + LBL_TER + "; -fx-font-size: 13px;");
        statusMsg.setWrapText(true);

        verifyCard.getChildren().addAll(vHeader, vSub, inputResi, btnVerify, statusMsg);

        // QR card
        VBox qrCard = iosCard();
        qrCard.setAlignment(Pos.CENTER);
        Label qHeader = sectionTitle("Payment QR Code");
        Label qSub    = sectionSub("Scan to confirm payment amount");

        StackPane qrFrame = new StackPane();
        qrFrame.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: " + SEPARATOR + "; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 16px; " +
            "-fx-padding: 14px;"
        );
        qrFrame.setMinHeight(190);
        qrFrame.setMaxWidth(210);
        qrFrame.setMinWidth(210);

        Label qrPlaceholder = new Label("QR akan tampil\nsetelah verifikasi");
        qrPlaceholder.setStyle(SF + "-fx-text-fill: " + LBL_TER + "; -fx-font-size: 12px; -fx-text-alignment: center;");
        qrPlaceholder.setAlignment(Pos.CENTER);

        qrView = new ImageView();
        qrView.setFitHeight(160);
        qrView.setPreserveRatio(true);

        qrFrame.getChildren().addAll(qrPlaceholder, qrView);
        qHeader.setMaxWidth(Double.MAX_VALUE);
        qSub.setMaxWidth(Double.MAX_VALUE);
        qrCard.getChildren().addAll(qHeader, qSub, qrFrame);

        // Pay card
        VBox payCard = iosCard();
        btnPay = iosButton("Authorize & Pay", GREEN);
        btnPay.setDisable(true);
        btnPay.setStyle(iosBtnDisabled());
        btnPay.setMaxWidth(Double.MAX_VALUE);

        footerStatus = new Label("Ready");
        footerStatus.setStyle(SF + "-fx-text-fill: " + LBL_TER + "; -fx-font-size: 12px;");
        footerStatus.setAlignment(Pos.CENTER);
        footerStatus.setMaxWidth(Double.MAX_VALUE);

        payCard.getChildren().addAll(btnPay, footerStatus);

        content.getChildren().addAll(verifyCard, qrCard, payCard);
        sp.setContent(content);
        outer.getChildren().add(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);

        // ── Events ────────────────────────────────────────────────────────────
        btnVerify.setOnAction(e -> {
            paketAktif = server.verifikasiResi(inputResi.getText().trim());

            if (paketAktif != null && !paketAktif.getStatusPembayaran().equals("Lunas")) {
                qrView.setImage(generateQR(loker.generateQRPembayaran(paketAktif)));
                qrPlaceholder.setVisible(false);
                animateFadeIn(qrView);
                statusMsg.setText("✓  Found — Rp " + String.format("%,.0f", paketAktif.getNominalCOD()));
                statusMsg.setStyle(SF + "-fx-text-fill: " + GREEN + "; -fx-font-size: 13px; -fx-font-weight: bold;");
                btnPay.setDisable(false);
                btnPay.setStyle(iosBtnStyle(GREEN));
                applyBtnShadow(btnPay, GREEN);
                setFooter("Package verified", GREEN);

            } else if (paketAktif != null && paketAktif.getStatusPembayaran().equals("Lunas")) {
                qrView.setImage(null);
                qrPlaceholder.setVisible(true);
                statusMsg.setText("⚠  Package already paid");
                statusMsg.setStyle(SF + "-fx-text-fill: " + ORANGE + "; -fx-font-size: 13px; -fx-font-weight: bold;");
                btnPay.setDisable(true);
                btnPay.setStyle(iosBtnDisabled());
                setFooter("Already paid", ORANGE);

            } else {
                qrView.setImage(null);
                qrPlaceholder.setVisible(true);
                statusMsg.setText("✗  Resi not found");
                statusMsg.setStyle(SF + "-fx-text-fill: " + RED + "; -fx-font-size: 13px; -fx-font-weight: bold;");
                btnPay.setDisable(true);
                btnPay.setStyle(iosBtnDisabled());
                shakeNode(inputResi);
                setFooter("Invalid resi", RED);
            }
        });

        btnPay.setOnAction(e -> {
            if (user.bayarCOD(paketAktif.getNominalCOD())) {
                server.updateStatusLunas(paketAktif.getNoResi());
                loker.bukaPintu();

                lblSaldo.setText("Rp " + String.format("%,.0f", user.getSaldoEWallet()));
                animateFadeIn(lblSaldo);

                qrView.setImage(null);
                qrPlaceholder.setVisible(true);
                statusMsg.setText("✓  Payment success! Locker unlocked.");
                statusMsg.setStyle(SF + "-fx-text-fill: " + GREEN + "; -fx-font-size: 13px; -fx-font-weight: bold;");
                btnPay.setDisable(true);
                btnPay.setStyle(iosBtnDisabled());
                inputResi.clear();
                setFooter("Locker unlocked ✓", GREEN);

                PauseTransition rst = new PauseTransition(Duration.seconds(4));
                rst.setOnFinished(ev -> {
                    statusMsg.setText("Awaiting package number...");
                    statusMsg.setStyle(SF + "-fx-text-fill: " + LBL_TER + "; -fx-font-size: 13px;");
                    setFooter("Ready", LBL_SEC);
                });
                rst.play();
            }
        });

        return outer;
    }

    // ── ADMIN VIEW ────────────────────────────────────────────────────────────
    private VBox buildAdminView() {
        VBox outer = new VBox();
        outer.setStyle("-fx-background-color: " + BG + ";");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 16, 32, 16));
        content.setStyle("-fx-background-color: " + BG + ";");

        // Admin gradient header card
        VBox adminHeader = new VBox(5);
        adminHeader.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " + PURPLE + ", #FF6CAE); " +
            "-fx-background-radius: 20px; " +
            "-fx-padding: 22px 22px;"
        );
        DropShadow ps = new DropShadow();
        ps.setColor(Color.web(PURPLE + "55")); ps.setOffsetY(8); ps.setRadius(20);
        adminHeader.setEffect(ps);

        Label aTitle = new Label("Database Management");
        aTitle.setStyle(SF + "-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label aSub = new Label("Register new packages to the system");
        aSub.setStyle(SF + "-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 13px;");
        adminHeader.getChildren().addAll(aTitle, aSub);

        // Form card
        VBox formCard = iosCard();

        Label resiLbl = new Label("Tracking Number");
        resiLbl.setStyle(SF + "-fx-text-fill: " + LBL_PRI + "; -fx-font-size: 15px; -fx-font-weight: bold;");
        TextField resiIn = new TextField();
        resiIn.setPromptText("e.g. JNE-2024-001");
        resiIn.setStyle(iosInputStyle());

        Label hargaLbl = new Label("COD Amount (Rp)");
        hargaLbl.setStyle(SF + "-fx-text-fill: " + LBL_PRI + "; -fx-font-size: 15px; -fx-font-weight: bold;");
        TextField hargaIn = new TextField();
        hargaIn.setPromptText("e.g. 75000");
        hargaIn.setStyle(iosInputStyle());

        Button btnSave = iosButton("Add Package", PURPLE);

        Label notifPill = new Label("");
        notifPill.setVisible(false);
        notifPill.setMaxWidth(Double.MAX_VALUE);
        notifPill.setAlignment(Pos.CENTER);

        btnSave.setOnAction(e -> {
            try {
                double harga = Double.parseDouble(hargaIn.getText().trim());
                if (server.tambahPaketBaru(resiIn.getText().trim(), harga)) {
                    showPill(notifPill, "✓  Package registered successfully", GREEN);
                    resiIn.clear(); hargaIn.clear();
                } else {
                    showPill(notifPill, "⚠  Duplicate resi number", ORANGE);
                    shakeNode(resiIn);
                }
            } catch (Exception ex) {
                showPill(notifPill, "✗  Amount must be numeric", RED);
                shakeNode(hargaIn);
            }
        });

        formCard.getChildren().addAll(resiLbl, resiIn, hargaLbl, hargaIn, btnSave, notifPill);
        content.getChildren().addAll(adminHeader, formCard);
        outer.getChildren().add(content);
        return outer;
    }

    // ── WALLET CARD ───────────────────────────────────────────────────────────
    private VBox buildWalletCard() {
        VBox card = new VBox(10);
        card.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #007AFF, #5AC8FA); " +
            "-fx-background-radius: 22px; " +
            "-fx-padding: 22px 22px 20px 22px;"
        );
        DropShadow sh = new DropShadow();
        sh.setColor(Color.web(BLUE + "55")); sh.setOffsetY(8); sh.setRadius(22);
        card.setEffect(sh);

        Label balLbl = new Label("E-Wallet Balance");
        balLbl.setStyle(SF + "-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 13px;");

        lblSaldo = new Label("Rp " + String.format("%,.0f", user.getSaldoEWallet()));
        lblSaldo.setStyle(SF + "-fx-text-fill: white; -fx-font-size: 30px; -fx-font-weight: bold;");

        HBox row = new HBox();
        row.setAlignment(Pos.BOTTOM_LEFT);

        VBox userInfo = new VBox(2);
        Label uLbl = new Label("OPERATOR");
        uLbl.setStyle(SF + "-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 10px; -fx-font-weight: bold;");
        Label uName = new Label(user.getNama());
        uName.setStyle(SF + "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        userInfo.getChildren().addAll(uLbl, uName);

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        Label badge = new Label("LOKER-001");
        badge.setStyle(SF +
            "-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; " +
            "-fx-background-color: rgba(255,255,255,0.22); " +
            "-fx-background-radius: 10px; -fx-padding: 4px 12px;"
        );

        row.getChildren().addAll(userInfo, sp2, badge);
        card.getChildren().addAll(balLbl, lblSaldo, row);
        return card;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private VBox iosCard() {
        VBox card = new VBox(12);
        card.setStyle(
            "-fx-background-color: " + BG_CARD + "; " +
            "-fx-background-radius: 20px; " +
            "-fx-padding: 18px 18px;"
        );
        DropShadow sh = new DropShadow();
        sh.setColor(Color.web("#00000012")); sh.setOffsetY(4); sh.setRadius(16);
        card.setEffect(sh);
        return card;
    }

    private Button iosButton(String text, String color) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(iosBtnStyle(color));
        applyBtnShadow(btn, color);
        return btn;
    }

    private void applyBtnShadow(Button btn, String color) {
        DropShadow sh = new DropShadow();
        sh.setColor(Color.web(color + "50")); sh.setOffsetY(4); sh.setRadius(12);
        btn.setEffect(sh);
    }

    private String iosBtnStyle(String color) {
        return SF +
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 16px; " +
            "-fx-background-radius: 14px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 14px 20px;";
    }

    private String iosBtnDisabled() {
        return SF +
            "-fx-background-color: " + SEPARATOR + "; " +
            "-fx-text-fill: " + LBL_TER + "; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 16px; " +
            "-fx-background-radius: 14px; " +
            "-fx-padding: 14px 20px;";
    }

    private String iosInputStyle() {
        return SF +
            "-fx-background-color: " + BG + "; " +
            "-fx-text-fill: " + LBL_PRI + "; " +
            "-fx-prompt-text-fill: " + LBL_TER + "; " +
            "-fx-border-color: " + SEPARATOR + "; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 12px; " +
            "-fx-background-radius: 12px; " +
            "-fx-padding: 13px 15px; " +
            "-fx-font-size: 15px;";
    }

    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle(SF + "-fx-text-fill: " + LBL_PRI + "; -fx-font-size: 17px; -fx-font-weight: bold;");
        return l;
    }

    private Label sectionSub(String text) {
        Label l = new Label(text);
        l.setStyle(SF + "-fx-text-fill: " + LBL_SEC + "; -fx-font-size: 13px;");
        return l;
    }

    private VBox makeTab(String icon, String label) {
        Label ic  = new Label(icon);
        ic.setStyle("-fx-font-size: 22px;");
        Label lb  = new Label(label);
        lb.setStyle(SF + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + LBL_SEC + ";");
        VBox tab = new VBox(2, ic, lb);
        tab.setAlignment(Pos.CENTER);
        tab.setPadding(new Insets(8, 0, 10, 0));
        tab.setCursor(javafx.scene.Cursor.HAND);
        return tab;
    }

    private String tabActiveStyle(String color) {
        return "-fx-background-color: " + BG_CARD + ";";
    }

    private String tabInactiveStyle() {
        return "-fx-background-color: " + BG_CARD + "; -fx-opacity: 0.38;";
    }

    private void showPill(Label pill, String msg, String color) {
        pill.setVisible(true);
        pill.setText(msg);
        pill.setStyle(
            SF + "-fx-font-size: 13px; -fx-font-weight: bold; " +
            "-fx-text-fill: " + color + "; " +
            "-fx-background-color: " + color + "18; " +
            "-fx-border-color: " + color + "44; " +
            "-fx-border-width: 1px; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-radius: 10px; " +
            "-fx-padding: 8px 14px;"
        );
        animateFadeIn(pill);
    }

    private void setFooter(String msg, String color) {
        if (footerStatus == null) return;
        footerStatus.setText(msg);
        footerStatus.setStyle(SF + "-fx-text-fill: " + color + "; -fx-font-size: 12px;");
    }

    private void animateFadeIn(javafx.scene.Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(350), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(55), node);
        tt.setFromX(0); tt.setToX(7);
        tt.setCycleCount(5);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
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