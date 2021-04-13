package com.example.ballsensorsgame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final int TIME_PERIOD = 20;  //Czas odswiezania ekranu
    private int minutes = 0;            //Minuty gry
    private int seconds = 0;            //Sekundy gry
    private int milliseconds = 0;       //Milisekundy gry

    private TextView scoreLabel, startLabel, livesLabel, caughtLabel;    //Labele na punkty, startowy, zycia, ilosci zlapanych kulek
    private ImageView box, pink, heart, blue, bomb;               //Elementy gry, koszyk, serce, pilki, bomba

    private int screenWidth;    //Szerokosc
    private int frameHeight;    //Wysokosc
    private int boxSize;        //Wielkosc kwadratowego koszyka

    private final float PINK_Y_BEGIN = -200; //Poczatkowe koordynaty Y elementow
    private final float HEART_Y_BEGIN = -12000;
    private final float BLUE_Y_BEGIN = -8000;
    private float boxX, boxY;   //Wspolrzedne koszyka
    private float pinkX, pinkY, heartX, heartY, blueY, blueX, bombY, bombX;   //Wspolrzedne pilek i bomby

    private float pinkSpeed;  //Szybkosc rozowej pilki - zmienia sie
    private float boxSpeed;   //Szybkosc koszyka - zalezne od wychylenia telefonu
    private float heartSpeed, blueSpeed, bombSpeed;    //Szybkosc serca, niebieskiej pilki i bomby - stale
    private float beginPinkSpeed;   //Poczatkowa predkosc rozowej pilki - potrzebna do dzialania niebieskiej pilki

    private int timePassed;     //Calkowita liczba milisekund jakie minely od poczatku

    //Potrzebne do wykonywania zmiany pozycji na okraglo
    private Timer timer = new Timer();  //Timer
    private final Handler handler = new Handler();  //Handler

    //Flaga czy zaczelismy gre - czy zostal wcisniety ekran
    private boolean start_flg = false;
    //Flaga czy rzucac bombe oraz parametry startu i okresu
    private boolean deploy_bomb = false;
    private final int START_BOMB = 30_000; //Od którego momentu rzucac bombami w milisekundach
    private final int PERIOD_BOMB = 20_000; //Co jaki czas rzucać bombe w milisekundach

    //Obiekt z dzwiekami gry
    private SoundPlayer soundPlayer;

    private final int MAXIMUM_DEATHS = 5; //Stala z liczba maksymalnych zginiec
    private int deathCounter = 0; //Licznik smierci
    private int caughtCounter = 0; //Licznik zlapanych rozowych kulek

    private float boxChangePositionX = 0; //Okresla o ile ma zmienic sie pozycja koszyka w osi X
    private float boxChangePositionY = 0; //Okresla o ile ma zmienic sie pozycja koszyka w osi y

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        soundPlayer = new SoundPlayer(this);

        scoreLabel = findViewById(R.id.scoreLabel);
        livesLabel = findViewById(R.id.livesLabel);
        startLabel = findViewById(R.id.startLabel);
        caughtLabel = findViewById(R.id.caughtLabel);
        box = findViewById(R.id.box);
        pink = findViewById(R.id.pink);
        heart = findViewById(R.id.heart);
        blue = findViewById(R.id.blue);
        bomb = findViewById(R.id.bomb);

        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        screenWidth = size.x;
        int screenHeight = size.y;

        beginPinkSpeed = Math.round(screenWidth / 1000.0f);
        boxSpeed = Math.round(screenHeight / 100.0f);
        pinkSpeed = beginPinkSpeed;
        heartSpeed = Math.round(screenWidth / 100.0f);
        blueSpeed = Math.round(screenWidth / 200.0f);
        bombSpeed = Math.round(screenWidth / 66.0f);

        scoreLabel.setText(getString(R.string.time, minutes, seconds, milliseconds));
        livesLabel.setText(getString(R.string.lives, MAXIMUM_DEATHS - deathCounter));
        caughtLabel.setText(getString(R.string.caught, caughtCounter));

        SensorManager mSrMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSrMgr.registerListener(this, mSrMgr.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void changePos() {
        int periodIncreaseSpeedPink = TIME_PERIOD * 150; //Czas co jaki ma być zwiekszana predkosc rozowej kulki
        hitCheck(); //Sprawdzenie czy nic nie wpadlo do koszyka
        timePassed += TIME_PERIOD; //Zwiekszenie czasu gry
        if (timePassed % periodIncreaseSpeedPink == 0) //Zwiekszanie co dany czas szybkosci rozowej kulki
            pinkSpeed += 0.95;

        if(timePassed >= START_BOMB && timePassed % PERIOD_BOMB == 0) //Warunki rzucenia bomby
            deploy_bomb = true;

        //Ustawienie czasu jaki minal od rozpoczecia gry
        minutes = (int) TimeUnit.MILLISECONDS.toMinutes(timePassed);
        seconds = (int) ((int) TimeUnit.MILLISECONDS.toSeconds(timePassed) -
                TimeUnit.MINUTES.toSeconds(minutes));
        milliseconds = timePassed - seconds * 1000 - minutes * 60000;

        //Ruch rozowej pilki
        pinkY += pinkSpeed;
        if (pinkY > frameHeight) { //Jezeli doleciala do konca
            //Znowu zaczyna leciec z gory
            pinkX = (float) Math.floor(Math.random() * (screenWidth - pink.getWidth()));
            pinkY =  PINK_Y_BEGIN / pinkSpeed;

            //Zwieksza sie licznik smierci
            deathCounter += 1;

            //Dzwiek ominiecia rozowej pilki
            soundPlayer.playMissedSound();

            //Aktualizacja ilosci zyc
            livesLabel.setText(getString(R.string.lives, MAXIMUM_DEATHS - deathCounter));
            if(deathCounter >= MAXIMUM_DEATHS){ //Jezeli gracz zginal za duzo razy
                endGame();
            }
        }
        //Nowe koordynaty rozowej pilki
        pink.setX(pinkX);
        pink.setY(pinkY);

        //Ruch serca
        heartY += heartSpeed;
        if (heartY > frameHeight) { //Jezeli dolecialo do konca znow z gory
            beginHeartPlace();
        }
        //Nowe koordynaty serca
        heart.setX(heartX);
        heart.setY(heartY);

        //Ruch niebieskiej kulki
        blueY += blueSpeed;
        if (blueY > frameHeight) { //Jezeli doleciala do konca znow z gory
            blueX = (float) Math.floor(Math.random() * (screenWidth - heart.getWidth()));
            blueY = BLUE_Y_BEGIN / pinkSpeed; //Wyskosc zalezy od szybkoscy rozowej - im szybciej tym blizej
        }
        //Nowe koordynaty niebieskiej
        blue.setX(blueX);
        blue.setY(blueY);

        //Ruch bomby, jezeli flaga rozlozenia bomby na True!
        if(deploy_bomb) {
            bombY += bombSpeed;
            if (bombY > frameHeight) { //Jezeli doleciala do konca znow z gory
                beginBombPlace();
                deploy_bomb = false;
            }
        }
        //Nowe koordynaty bomby
        bomb.setX(bombX);
        bomb.setY(bombY);

        //Ruch koszyka
        boxX -= boxChangePositionX;
        boxY += boxChangePositionY;

        //Granice ruch koszyka, aby gracz nie wyszedl poza plansze
        if (boxX < 0) boxX = 0;
        if (boxX > screenWidth - boxSize) boxX = screenWidth - boxSize;
        if (boxY > frameHeight - boxSize) boxY = frameHeight - boxSize;
        if (boxY < 0.5f*frameHeight) boxY = 0.5f*frameHeight;

        //Ustawienie koszyka
        box.setX(boxX);
        box.setY(boxY);

        //Uaktualniony czas
        scoreLabel.setText(getString(R.string.time, minutes, seconds, milliseconds));
    }

    public void hitCheck() {
        float pinkCenterX = pinkX + pink.getWidth() / 2.0f;
        float pinkCenterY = pinkY + pink.getHeight() / 2.0f;

        //Jezeli trafiono rozowa kulke, ta znowu na gorze
        if (boxY <= pinkCenterY && pinkCenterY <= boxY + boxSize &&
                boxX <= pinkCenterX && pinkCenterX <= boxX + boxSize) {
            beginPinkPlace();
            soundPlayer.playHitSound();
            caughtCounter++;
            caughtLabel.setText(getString(R.string.caught, caughtCounter));
        }

        float heartCenterX = heartX + heart.getWidth() / 2.0f;
        float heartCenterY = heartY + heart.getHeight() / 2.0f;

        //Jezeli trafiono serce, +1 zycie, miejsce z ktorego startuje spadek zalezy od ilosci smierci
        if (boxY <= heartCenterY && heartCenterY <= boxY + boxSize &&
                boxX <= heartCenterX && heartCenterX <= boxX + boxSize) {
            heartX = (float) Math.floor(Math.random() * (screenWidth - heart.getWidth()));
            if (deathCounter > 0) //Ulatwienie - im wiecej smierci tym serce spadnie szybciej
                heartY = HEART_Y_BEGIN / deathCounter;
            else
                heartY = HEART_Y_BEGIN;
            deathCounter -= 1; //Zmniejszenie licznika smierci
            livesLabel.setText(getString(R.string.lives, MAXIMUM_DEATHS - deathCounter));
            soundPlayer.playHitSound();
        }

        float blueCenterX = blueX + blue.getWidth() / 2.0f;
        float blueCenterY = blueY + blue.getHeight() / 2.0f;

        //Jezeli trafiono niebieska kulke, predkosc rozowej zmniejsza sie do wartosci poczatkowej
        if (boxY <= blueCenterY && blueCenterY <= boxY + boxSize &&
                boxX <= blueCenterX && blueCenterX <= boxX + boxSize) {
            pinkSpeed = beginPinkSpeed;
            beginBluePlace();
            soundPlayer.playHitSound();
        }

        float bombCenterX = bombX + bomb.getWidth() / 2.0f;
        float bombCenterY = bombY + bomb.getHeight() / 2.0f;

        //Jezeli trafiono bombe koniec gry
        if (boxY <= bombCenterY && bombCenterY <= boxY + boxSize &&
                boxX <= bombCenterX && bombCenterX <= boxX + boxSize) {
            endGame();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!start_flg) {
            start_flg = true;

            FrameLayout frameLayout = findViewById(R.id.frame);
            frameHeight = frameLayout.getHeight();

            boxX = box.getX();
            boxY = box.getY();
            boxSize = box.getHeight();
            box.setY(frameHeight - boxSize);

            //Ustawienie poczatkowe wszystkich kulek
            beginPinkPlace();
            beginHeartPlace();
            beginBluePlace();
            beginBombPlace();

            startLabel.setVisibility(View.GONE);

            //Rozpoczecie wlasciwej gry
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.post(() -> changePos());
                }
            }, 0, TIME_PERIOD);

        }
        return super.onTouchEvent(event);
    }

    private void endGame(){
        if (timer != null) {    //Koniec nieskonczonej petli
            timer.cancel();
            timer = null;
        }
        //Dzwiek game over!
        soundPlayer.playGameOverSound();
        //Przejscie do aktywnosci z wynikiem
        Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
        intent.putExtra("MINUTES", minutes);
        intent.putExtra("SECONDS", seconds);
        intent.putExtra("MILLISECONDS", milliseconds);
        startActivity(intent);
    }

    private void beginBluePlace(){
        blueX = (float) Math.floor(Math.random() * (screenWidth - blue.getWidth()));
        blueY = BLUE_Y_BEGIN;
    }

    private void beginHeartPlace(){
        heartX = (float) Math.floor(Math.random() * (screenWidth - heart.getWidth()));
        heartY = HEART_Y_BEGIN;
    }

    private void beginPinkPlace(){
        pinkX = (float) Math.floor(Math.random() * (screenWidth - pink.getWidth()));
        pinkY = PINK_Y_BEGIN;
    }

    private void beginBombPlace(){
        bombX = (float) Math.floor(Math.random() * (screenWidth - bomb.getWidth()));
        bombY = -200;
    }

    @Override
    public void onBackPressed() {}

    @Override
    public void onSensorChanged(SensorEvent event) {
        float earthAcceleration = 9.81f;
        boxChangePositionX = ((event.values[0] * boxSpeed) / earthAcceleration);
        boxChangePositionY = ((event.values[1] * boxSpeed) / earthAcceleration);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}