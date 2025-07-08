package com.textifyer.audio;

import android.media.MediaPlayer;

import java.io.File;
import java.io.IOException;

public class AudioPlayer {
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    public void play(File audioFile, boolean startImmediately, Runnable onStart, Runnable onComplete, Runnable onError) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    onError.run();
                    return true;
                });
            } else {
                mediaPlayer.reset();
            }

            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                if (startImmediately) {
                    mp.start();
                    isPlaying = true;
                    onStart.run();
                } else {
                    onStart.run();
                }
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                onComplete.run();
            });
        } catch (IOException e) {
            onError.run();
        }
    }

    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
        }
    }

    public void resume() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer.start();
            isPlaying = true;
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
