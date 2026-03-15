package com.securebox.models;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class SecureBoxLocker {
    private String idLocker;
    private boolean isPintuTerbuka;
    
    // Konfigurasi server MQTT Publik (Gratis)
    private static final String BROKER = "tcp://broker.hivemq.com:1883";
    private static final String TOPIC = "securebox/qori/loker001"; // Ini "frekuensi" radio rahasia lu

    public SecureBoxLocker(String idLocker) {
        this.idLocker = idLocker;
        this.isPintuTerbuka = false;
    }

    public String generateQRPembayaran(Paket paket) {
        return "QR-PAYMENT-TOKEN:" + paket.getNoResi() + "-Rp" + paket.getNominalCOD();
    }

    public void bukaPintu() {
        this.isPintuTerbuka = true;
        System.out.println(">> [SISTEM] Memproses pembukaan solenoid...");
        
        // Mengirim sinyal ke server MQTT
        try {
            MqttClient client = new MqttClient(BROKER, MqttClient.generateClientId(), new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            
            System.out.println(">> [IoT] Menyambungkan ke Broker HiveMQ...");
            client.connect(options);
            
            String payload = "BUKA_PINTU";
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(2);
            
            client.publish(TOPIC, message);
            System.out.println(">> [IoT] Sinyal 'BUKA_PINTU' berhasil dipancarkan ke Wokwi!");
            
            client.disconnect();
        } catch (Exception e) {
            System.out.println(">> [IoT ERROR] Gagal mengirim sinyal: " + e.getMessage());
        }
    }
}