package com.myapp.ceromusicapp.Helpers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.myapp.ceromusicapp.MyMediaPlayer;

public class AudioFocusHelper {
    private static AudioManager audioManager;
    private static final MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();

    public static void initializeAudioManager(Context context) {
        if (audioManager == null)
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public static boolean isAudioFocusGranted() {
        return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    // Audio attributes instance to set the playback
    // attributes for the media player instance
    // these attributes specify what type of media is
    // to be played and used to callback the audioFocusChangeListener
    private static AudioAttributes getPlaybackAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
    }

    // media player is handled according to the
    // change in the focus which Android system grants for
    final static AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
            i -> {

                switch (i) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        Log.d("-AudioFocusChangeListener", "------------------ AUDIOFOCUS_GAIN ------------------");
                        mediaPlayer.setVolume(1f, 1f);
                        startPlayer();
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS:
                        Log.d("-AudioFocusChangeListener", "------------------ AUDIOFOCUS_LOSS ------------------");
                        pausePlayer();
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        Log.d("-AudioFocusChangeListener", "------------------ AUDIOFOCUS_LOSS_TRANSIENT " +
                                "------------------");
                        pausePlayer();
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        Log.d("-AudioFocusChangeListener", "------------------ AUDIOFOCUS_LOSS_TRANSIENT " +
                                "------------------");
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.setVolume(0.3f, 0.3f);
                        }
                        break;
                }
            };

    private static final AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(getPlaybackAttributes())
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build();

    public static void release() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest);
        audioManager = null;
    }

//    Helper functions to start/pause mediaplayer instance
//    without checking any extra conditions unlike in MyMediaPlayer.class
    private static void startPlayer() {
        mediaPlayer.start();
        MediaSessionHelper.updatePlaybackState(MyMediaPlayer.mediaSession, PlaybackStateCompat.STATE_PLAYING);
    }

    private static void pausePlayer() {
        mediaPlayer.pause();
        MediaSessionHelper.updatePlaybackState(MyMediaPlayer.mediaSession, PlaybackStateCompat.STATE_PAUSED);
    }

}
