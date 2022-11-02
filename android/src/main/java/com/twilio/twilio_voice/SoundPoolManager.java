package com.twilio.twilio_voice;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import static android.content.Context.AUDIO_SERVICE;

public class SoundPoolManager {

    private boolean playing = false;
    private boolean loaded = false;
    private boolean playingCalled = false;
    private final float volume;
    private SoundPool soundPool;
    private final int ringingSoundId;
    private int ringingStreamId;
    private final int disconnectSoundId;
    private static SoundPoolManager instance;
    private final Vibrator vibrator;
    private final AudioManager audioManager;
    private SoundPoolManager(Context context) {
        // AudioManager audio settings for adjusting the volume
        audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = actualVolume / maxVolume;

        // Load the sounds
        int maxStreams = 1;
        soundPool = new SoundPool.Builder()
          .setMaxStreams(maxStreams)
          .build();

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
               if (playingCalled && sampleId == ringingSoundId) {
                    playRinging();
                    playingCalled = false;
                }
            }

        });
        ringingSoundId = soundPool.load(context, R.raw.incoming, 1);
        disconnectSoundId = soundPool.load(context, R.raw.disconnect, 1);
        // Get instance of Vibrator from current Context
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static SoundPoolManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundPoolManager(context);
        }
        return instance;
    }

    public void playRinging() {
        if (loaded && !playing) {

            switch (audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_NORMAL:
                    ringingStreamId = soundPool.play(ringingSoundId, volume, volume, 1, -1, 1f);
                    vibrate();
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    vibrate();
                    break;
                default:
                    break;
            }
            playing = true;
        } else {
            playingCalled = true;
        }
    }
    private void vibrate(){
        long[] mVibratePattern = new long[]{0, 400, 400, 400, 400, 400, 400, 400};
        //final int[] mAmplitudes = new int[]{0, 128, 0, 128, 0, 128, 0, 128};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator.vibrate(VibrationEffect.createWaveform(mVibratePattern, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vibrator.vibrate(mVibratePattern, 0);
        }
    }

    public void stopRinging() {
        if (playing) {
            soundPool.stop(ringingStreamId);
            vibrator.cancel();
            playing = false;
        }
    }

    public void playDisconnect() {
        if (loaded && !playing) {
            soundPool.play(disconnectSoundId, volume, volume, 1, 0, 1f);
            playing = false;
        }
    }

    public void release() {
        if (soundPool != null) {
            soundPool.unload(ringingSoundId);
            soundPool.unload(disconnectSoundId);
            soundPool.release();
            soundPool = null;
        }
        instance = null;
    }

}
