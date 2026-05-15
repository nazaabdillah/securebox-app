package com.securebox.models;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class SecureBoxLocker {

    private String idLocker;
    private boolean isPintuTerbuka;

    // =============================================
    // GANTI KE BROKER LOKAL (Mosquitto di laptop)
    // =============================================
    private static final String BROKER      = "tcp://broker.hivemq.com:1883";
    private static final String TOPIC_CMD   = "securebox/loker001/cmd";
    private static final String TOPIC_STATUS = "securebox/loker001/status";

    public SecureBoxLocker(String idLocker) {
        this.idLocker      = idLocker;
        this.isPintuTerbuka = false;
    }

    // =============================================
    // GENERATE QR TOKEN PEMBAYARAN
    // =============================================
    public String generateQRPembayaran(Paket paket) {
        return "QR-PAYMENT-TOKEN:" + paket.getNoResi() + "-Rp" + paket.getNominalCOD();
    }

    // =============================================
    // CEK KONEKSI KE BROKER (opsional, buat debug)
    // =============================================
    public boolean cekKoneksi() {
        try {
            MqttClient client = new MqttClient(BROKER, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(3);
            client.connect(options);
            boolean connected = client.isConnected();
            client.disconnect();
            System.out.println(">> [IoT] Broker status: " + (connected ? "ONLINE" : "OFFLINE"));
            return connected;
        } catch (Exception e) {
            System.out.println(">> [IoT] Broker OFFLINE: " + e.getMessage());
            return false;
        }
    }

    // =============================================
    // BUKA PINTU — kirim MQTT ke ESP32
    // =============================================
    public void bukaPintu() {
        this.isPintuTerbuka = true;
        System.out.println(">> [SISTEM] Memproses aktuasi solenoid...");

        new Thread(() -> {
            try {
                MqttClient client = new MqttClient(
                    BROKER,
                    MqttClient.generateClientId(),
                    new MemoryPersistence()
                );

                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setConnectionTimeout(5);
                options.setKeepAliveInterval(10);

                System.out.println(">> [IoT] Menyambungkan ke broker lokal...");
                client.connect(options);

                // Kirim perintah buka pintu
                MqttMessage perintah = new MqttMessage("BUKA_PINTU".getBytes());
                perintah.setQos(1); // QoS 1: guaranteed delivery
                perintah.setRetained(false);
                client.publish(TOPIC_CMD, perintah);
                System.out.println(">> [IoT] Payload BUKA_PINTU terkirim ke ESP32!");

                client.disconnect();
                System.out.println(">> [IoT] Koneksi broker ditutup.");

            } catch (Exception e) {
                System.out.println(">> [IoT ERROR] Gagal kirim ke ESP32: " + e.getMessage());
                System.out.println(">> [IoT] Pastikan Mosquitto sudah berjalan di laptop!");
            }
        }).start();
    }

    // =============================================
    // GETTER
    // =============================================
    public String getIdLocker() {
        return idLocker;
    }

    public boolean isPintuTerbuka() {
        return isPintuTerbuka;
    }
}