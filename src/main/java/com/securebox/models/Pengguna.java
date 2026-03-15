package com.securebox.models;

public class Pengguna {
    private String nama;
    private String idPengguna;
    private double saldoEWallet;

    public Pengguna(String idPengguna, String nama, double saldoAwal) {
        this.idPengguna = idPengguna;
        this.nama = nama;
        this.saldoEWallet = saldoAwal;
    }

    public String getNama() {
        return nama;
    }

    public double getSaldoEWallet() {
        return saldoEWallet;
    }

    // Logika Pemotongan Saldo COD
    public boolean bayarCOD(double nominal) {
        if (this.saldoEWallet >= nominal) {
            this.saldoEWallet -= nominal;
            return true; // Saldo cukup, pembayaran berhasil
        } else {
            return false; // Saldo kurang, gagal
        }
    }
}