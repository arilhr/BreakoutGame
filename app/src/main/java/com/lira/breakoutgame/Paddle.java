package com.lira.breakoutgame;

import android.graphics.RectF;

public class Paddle {
    // Objek yang menyimpan empat koordinat untuk membuat kotak tongkat pemukul
    private RectF rect;

    // Panjang dan tinggi tongkat pemukul
    private long length;
    private long height;

    // Lokasi dari tongkat pemukul
    private float x;
    private float y;

    // Kecepetan gerak tongkat pemukul dalam pixel per detik
    private float paddleSpeed;

    // Menyimpan arah gerak yang bisa dilakukan oleh tongkat pemukul
    public final int STOPPED = 0;
    public final int LEFT = 1;
    public final int RIGHT = 2;

    // Menyimpan pergerakan tongkat pemukul ( berhenti / ke kanan /ke  kiri )
    private int paddleMoving = STOPPED;

    // Method konstruktor
    // Method ini dijalankan saat kita membuat objek dari kelas ini
    // Disini kita akan mengambil nilai panjang dan lebar layar
    public Paddle(int screenX, int screenY) {
        // Atur nilai panjang 130 px dan tinggi 20 px untuk tongkat pemukul
        length = 130;
        height = 20;

        // Atur lokasi pemukul di tengah layar saat dimulai
        x = screenX / 2f - (length / 2f);
        y = screenY - 20;

        rect = new RectF(x, y, x + length, y + height);

        // Tentukan kecepatan pemukul
        paddleSpeed = 350;
    }

    // Method getter untuk membuat kotak yang
    // mendefinisikan tongkat pemukul kita ada di kelas BreakoutView
    public RectF getRect() {
        return rect;
    }

    // Method untuk menentukan jika pemukul bergerak ke kanan, ke kiri, atau berhenti
    public void setMovementState(int state) {
        paddleMoving = state;
    }

    // Method ini akan dipanggil di update pada BreakoutView
    // Digunakan untuk menentukan jika pemukul perlu untuk bergerak dan mengubah koordinat
    public void update(long fps, int screenX) {
        if (paddleMoving == LEFT && x >= 0) {
            x = x - paddleSpeed / fps;
        }
        if (paddleMoving == RIGHT && x <= screenX - length) {
            x = x + paddleSpeed / fps;
        }

        rect.left = x;
        rect.right = x + length;
    }

    public void reset(int screenX) {
        x = screenX / 2f - (length / 2f);
        rect.left = x;
        rect.right = x + length;
    }
}
