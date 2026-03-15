package com.securebox.models;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseHelper {
    // Nama file database-nya nanti
    private static final String URL = "jdbc:sqlite:securebox.db";

    // Fungsi untuk menyambungkan aplikasi ke file database
    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (Exception e) {
            System.out.println("Koneksi Database Gagal: " + e.getMessage());
        }
        return conn;
    }

    // Fungsi untuk membuat tabel otomatis kalau belum ada
    public static void inisialisasiDatabase() {
        String sqlTabelPaket = "CREATE TABLE IF NOT EXISTS paket ("
                + "no_resi TEXT PRIMARY KEY, "
                + "nominal_cod REAL NOT NULL, "
                + "status_pembayaran TEXT NOT NULL"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            
            // Mengeksekusi pembuatan tabel
            stmt.execute(sqlTabelPaket);
            System.out.println(">> Database SQLite berhasil diinisialisasi!");
            
            // Masukin satu data contoh (Dummy awal) biar nggak kosong banget
            stmt.execute("INSERT OR IGNORE INTO paket (no_resi, nominal_cod, status_pembayaran) VALUES ('RESI123', 50000.0, 'Menunggu');");
            
        } catch (Exception e) {
            System.out.println("Gagal membuat tabel: " + e.getMessage());
        }
    }
}