package com.example.ballsensorsgame;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

class SoundPlayer {
    private static SoundPool soundPool;
    private static int hitSound, overSound, gameOver;

    SoundPlayer(Context context) {

        // SoundPool is deprecated in API level 21.(Lollipop)
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(2)
                .build();

        hitSound = soundPool.load(context, R.raw.hit, 1);
        overSound = soundPool.load(context, R.raw.over, 1);
        gameOver = soundPool.load(context, R.raw.game_over, 1);
    }

    void playHitSound() {
        soundPool.play(hitSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    void playMissedSound() {
        soundPool.play(overSound, 1.0f, 1.0f, 1, 0, 1.0f);
    }

    void playGameOverSound(){
        soundPool.play(gameOver, 1.0f, 1.0f, 1, 0, 1.0f);
    }
}
