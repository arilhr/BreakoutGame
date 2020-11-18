package com.lira.breakoutgame;

// import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class BreakoutGame extends Activity {

    // breakoutView sebagai tampilan utama game
    // yang juga berisi logika utama game
    // dan juga yang merespon sentuhan layar dari pemain
    BreakoutView breakoutView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inisialisasi breakoutView menjadi tampilan utama
        breakoutView = new BreakoutView(this);
        setContentView(breakoutView);

    }

    class BreakoutView extends SurfaceView implements Runnable {

        // Thread
        Thread gameThread = null;

        // SurfaceHolder berguna saat menggunakan Paint dan Canvas di Thread
        // bisa dilihat nanti saat menjalankan method draw.
        SurfaceHolder ourHolder;

        // Bollean yang berfungsi menentukan game berjalan atau tidak
        volatile boolean playing;

        // Game di pause pada awal mulai
        boolean paused = true;

        // Objek canvas dan paint
        Canvas canvas;
        Paint paint;

        // variabel yang menyimpan frame rate
        long fps;

        // Untuk membantu menghitung fps
        private long timeThisFrame;

        // Ukuran layar dalam pixel
        int screenX;
        int screenY;

        // Tongkat pemukul pemain
        Paddle paddle;

        // Bola
        Ball ball;

        // sampai dengan 200 balok bata
        Brick[] bricks = new Brick[200];
        int numBricks = 0;

        // Efek suara
        SoundPool soundPool;
        int beep1ID = -1;
        int beep2ID = -1;
        int beep3ID = -1;
        int loseLifeID = -1;
        int explodeID = -1;

        // Skor
        int score = 0;

        // Nyawa
        int lives = 3;

        // Method yang jalan saat kita memanggil new() breakoutView
        public BreakoutView(Context context) {
            // Meminta SurfaceView untuk mengetur objek kita.
            super(context);

            // Inisialisasi objek ourHolder dan paint
            ourHolder = getHolder();
            paint = new Paint();

            // Objek Display untuk mendapatkan detail layar
            Display display = getWindowManager().getDefaultDisplay();
            // Simpan resolusi di objek Point
            Point size = new Point();
            display.getSize(size);

            screenX = size.x;
            screenY = size.y;

            paddle = new Paddle(screenX, screenY);

            // Inisialisasi bola
            ball = new Ball(screenX, screenY);

            // Memuat efek suara
            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

            try {
                // Buat objek dari dua kelas yang dibutuhkan
                AssetManager assetManager = context.getAssets();
                AssetFileDescriptor descriptor;

                // Muat efek suara kita di memory yang siap digunakan
                descriptor = assetManager.openFd("beep1.ogg");
                beep1ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep2.ogg");
                beep2ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep3.ogg");
                beep3ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("loseLife.ogg");
                loseLifeID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("explode.ogg");
                explodeID = soundPool.load(descriptor, 0);
            } catch (IOException e) {
                // Tampilkan pesan error ke konsol
                Log.e("error:", "failed to load sound file");
            }

            createBricksAndRestart();
        }

        public void createBricksAndRestart() {

            // Letakkan bola ke lokasi awal
            ball.reset(screenX, screenY);
            paddle.reset(screenX);

            int brickWidth = screenX / 8;
            int brickHeight = screenY / 10;

            // Membuat sekumpulan balok bata
            numBricks = 0;
            for (int column = 0; column < 8; column++) {
                for (int row = 0; row < 3; row++) {
                    bricks[numBricks] = new Brick(row, column, brickWidth, brickHeight);
                    numBricks++;
                }
            }

            // Reset skor dan nyawa
            if (lives == 0) {
                score = 0;
                lives = 3;
            }
        }

        @Override
        public void run() {
            while (playing) {

                // Dapatkan waktu saat ini dalam milliseconds
                long startFrameTime = System.currentTimeMillis();

                // Perbarui frame
                if(!paused){
                    update();
                }

                // Menggambar frame
                draw();

                // Hitung fps pada frame saat ini
                // Kita bisa gunakan untuk waktu animasi dan lainnya
                timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = 1000 / timeThisFrame;
                }
            }

        }

        // Semua yang perlu diupdate diletakkan disini
        // seperti gerakan, tabrakan, dll.
        public void update() {
            // Gerakkan paddle bila diperlukan
            paddle.update(fps, screenX);

            // Gerakkan bola
            ball.update(fps);

            // Cek saat bola bertabrakan dengan balok bata
            for(int i = 0; i < numBricks; i++){
                if (bricks[i].getVisibility()) {
                    if(RectF.intersects(bricks[i].getRect(), ball.getRect())) {
                        bricks[i].setInvisible();
                        ball.reverseYVelocity();
                        score = score + 10;
                        soundPool.play(explodeID, 1, 1, 0, 0, 1);
                    }
                }
            }

            // Cek saat bola bertabrakan dengan tongkat pemukul
            if(RectF.intersects(paddle.getRect(), ball.getRect())) {
                ball.setRandomXVelocity();
                ball.reverseYVelocity();
                ball.clearObstacleY(paddle.getRect().top - 2);
                soundPool.play(beep1ID, 1, 1, 0, 0, 1);
            }

            // Pantulkan bola kembali saat menyentuh bagian bawah layar
            // dan kurangi nyawa pemain
            if (ball.getRect().bottom > screenY) {
                ball.reverseYVelocity();
                ball.clearObstacleY(screenY - 2);

                // Kurangi nyawa
                lives--;
                soundPool.play(loseLifeID, 1, 1, 0, 0, 1);

                if (lives == 0)
                    paused = true;
            }

            // Pantulkan bola jika mengenai bagian atas layar
            if (ball.getRect().top < 0) {
                ball.reverseYVelocity();
                ball.clearObstacleY(12);
                soundPool.play(beep2ID, 1, 1, 0, 0, 1);
            }

            // Pantulkan bola jika mengenai bagian kiri layar
            if (ball.getRect().left < 0){
                ball.reverseXVelocity();
                ball.clearObstacleX(2);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // Pantulkan bola jika mengenai bagian kanan layar
            if (ball.getRect().right > screenX - 10){
                ball.reverseXVelocity();
                ball.clearObstacleX(screenX - 22);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // Pause jika sudah membersihkan semua balok bata
            if (score == numBricks * 10)
                paused = true;
        }

        // Gambar tampilan yang terbaru
        public void draw() {

            // Memastikan surface menggambar itu valid
            if (ourHolder.getSurface().isValid()) {
                // Kunci, dan kanvas siap digunakan
                canvas = ourHolder.lockCanvas();

                // Gambar warna background
                canvas.drawColor(Color.argb(255,  26, 128, 182));

                // Pilih warna kuas untuk menggambar
                paint.setColor(Color.argb(255,  255, 255, 255));

                // Gambar tongkat pemukul
                canvas.drawRect(paddle.getRect(), paint);

                // Gambar bola
                canvas.drawRect(ball.getRect(), paint);

                // Gambar balok bata
                // Ubah warna kuas237161198
                paint.setColor(Color.argb(255,245,83,210));

                // Gambar balok bata jika visible
                for (int i = 0; i < numBricks; i++) {
                    if (bricks[i].getVisibility()) {
                        canvas.drawRect(bricks[i].getRect(), paint);
                    }
                }

                // Gambar HUD
                // Pilih warna kuas untuk menggambar HUD
                paint.setColor(Color.argb(255,255,255,255));

                // Gambar skor dan nyawa
                paint.setTextSize(40);
                canvas.drawText("Score: " + score + "  Lives: " + lives, 10, 50, paint);

                // Apakah pemain telah menghabiskan balok bata ?
                if (score == numBricks * 10) {
                    paint.setTextSize(90);
                    canvas.drawText("YOU HAVE WON!", 50, screenY / 2f, paint);
                }

                // Apakah pemain kehilangan semua nyawa ?
                if (lives <= 0) {
                    paint.setTextSize(90);
                    canvas.drawText("YOU HAVE LOST!", 50, screenY / 2f, paint);
                }

                // Gambar semuanya kedalam tampilan
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        // Jika Activity di pause/stop
        // matikan thread kita.
        public void pause() {
            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }

        }

        // Jika Activity dimulai
        // mulai thread kita.
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        // Kelas SurfaceView mempunyai method onTouchListener
        // jadi kita bisa men-override method tersebut dan mendeteksi sentuhan layar.
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

                // Pemain menyentuh layar
                case MotionEvent.ACTION_DOWN:
                    if (lives <= 0 || score == numBricks * 10)
                        createBricksAndRestart();
                    else
                        paused = false;

                    if (motionEvent.getX() > screenX / 2f) {
                        paddle.setMovementState(paddle.RIGHT);
                    } else {
                        paddle.setMovementState(paddle.LEFT);
                    }
                    break;

                // Pemain mengangkat sentuhan jari dari layar
                case MotionEvent.ACTION_UP:

                    paddle.setMovementState(paddle.STOPPED);
                    break;
            }
            return true;
        }

    }
    // Akhir dari Inner Class BreakoutView

    // Method ini dijalankan saat pemain memulai game
    @Override
    protected void onResume() {
        super.onResume();

        // Beritahu breakoutView untuk menjalankan method resume
        breakoutView.resume();
    }

    // Method ini dijalankan saat pemain keluar dari game
    @Override
    protected void onPause() {
        super.onPause();

        // Beritahu breakoutView untuk menjalankan method pause
        breakoutView.pause();
    }

}