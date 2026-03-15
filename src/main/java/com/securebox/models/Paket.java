package com.securebox.models;

public class Paket {
    private String noResi;
    private double nominalCOD;
    private String statusPembayaran; // Contoh: "Menunggu", "Lunas"

    // Constructor (Fungsi yang pertama dipanggil pas paket dibuat)
    public Paket(String noResi, double nominalCOD) {
        this.noResi = noResi;
        this.nominalCOD = nominalCOD;
        this.statusPembayaran = "Menunggu";
    }

    // Getter & Setter (Jalur resmi buat ngambil/ngubah data)
    public String getNoResi() {
        return noResi;
    }

    public double getNominalCOD() {
        return nominalCOD;
    }

    public String getStatusPembayaran() {
        return statusPembayaran;
    }

    public void setStatusPembayaran(String status) {
        this.statusPembayaran = status;
    }
}