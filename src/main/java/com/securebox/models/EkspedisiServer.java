package com.securebox.models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EkspedisiServer {

    // Constructor kosong, karena kita udah nggak butuh ArrayList dummy lagi
    public EkspedisiServer() {
    }

    // Fungsi utama: Kurir ngecek resi, tapi sekarang nyarinya ke Database!
    public Paket verifikasiResi(String noResi) {
        // Query SQL buat nyari resi spesifik
        String sql = "SELECT * FROM paket WHERE no_resi = ?";

        // Buka koneksi ke SQLite
        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Masukin nomor resi yang diketik kurir ke dalam query SQL
            pstmt.setString(1, noResi);
            
            // Eksekusi pencarian
            ResultSet rs = pstmt.executeQuery();

            // Kalau datanya ketemu di tabel
            if (rs.next()) {
                double nominal = rs.getDouble("nominal_cod");
                String status = rs.getString("status_pembayaran");
                
                // Bikin objek Paket dari data asli database
                Paket paketDitemukan = new Paket(noResi, nominal);
                paketDitemukan.setStatusPembayaran(status);
                
                return paketDitemukan; // Balikin datanya ke UI
            }
        } catch (Exception e) {
            System.out.println(">> ERROR Database: " + e.getMessage());
        }
        
        return null; // Kalau resi fiktif atau nggak ada di database
    }
    // Fungsi buat ngubah status di database jadi Lunas
    public void updateStatusLunas(String noResi) {
        String sql = "UPDATE paket SET status_pembayaran = 'Lunas' WHERE no_resi = ?";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, noResi); // Masukin nomor resinya
            pstmt.executeUpdate();      // Eksekusi perubahan ke SQLite
            
            System.out.println(">> DATABASE UPDATE: Paket " + noResi + " sekarang resmi LUNAS!");
            
        } catch (Exception e) {
            System.out.println(">> ERROR Update: " + e.getMessage());
        }
    }// Fungsi khusus ADMIN untuk nambah paket baru ke dalam Database
    public boolean tambahPaketBaru(String noResi, double nominal) {
        String sql = "INSERT INTO paket (no_resi, nominal_cod, status_pembayaran) VALUES (?, ?, 'Menunggu')";

        try (Connection conn = DatabaseHelper.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, noResi);
            pstmt.setDouble(2, nominal);
            pstmt.executeUpdate();
            
            System.out.println(">> DATABASE INSERT: Paket " + noResi + " berhasil ditambahkan!");
            return true;
            
        } catch (Exception e) {
            System.out.println(">> ERROR Insert: " + e.getMessage());
            return false;
        }
    }

}