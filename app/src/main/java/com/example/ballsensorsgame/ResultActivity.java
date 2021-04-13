package com.example.ballsensorsgame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView scoreLabel = findViewById(R.id.scoreLabel);
        TextView highScoreLabel = findViewById(R.id.highScoreLabel);

        int minutes = getIntent().getIntExtra("MINUTES", 0);
        int seconds = getIntent().getIntExtra("SECONDS", 0);
        int milliseconds = getIntent().getIntExtra("MILLISECONDS", 0);
        int timeSum = minutes * 60000 + seconds * 1000 + milliseconds;
        scoreLabel.setText(getString(R.string.result_time, minutes, seconds, milliseconds));

        // High Score
        SharedPreferences sharedPreferences = getSharedPreferences("GAME_DATA", Context.MODE_PRIVATE);
        int highScore = sharedPreferences.getInt("HIGH_SCORE", 0);

        if (timeSum > highScore) {
            // Uaktualnienie HighScore
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("HIGH_SCORE", timeSum);
            editor.apply();

        } else {
            minutes = (int) TimeUnit.MILLISECONDS.toMinutes(highScore);
            seconds = (int) ((int) TimeUnit.MILLISECONDS.toSeconds(highScore) -
                    TimeUnit.MINUTES.toSeconds(minutes));
            milliseconds = highScore - seconds * 1000 - minutes * 60000;
        }
        highScoreLabel.setText(getString(R.string.best_time, minutes, seconds, milliseconds));
    }

    public void tryAgain(View view) {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    @Override
    public void onBackPressed() {}
}