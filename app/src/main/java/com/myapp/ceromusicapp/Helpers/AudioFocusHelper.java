package com.myapp.ceromusicapp.Helpers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.myapp.ceromusicapp.MyMediaPlayer;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AudioFocusHelper {
    private static AudioManager audioManager;

    public static void initializeAudioManager(Context context) {
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
                        Log.d("AudioFocusChangeListener", "------------------ AUDIOFOCUS_GAIN ------------------");
                        MyMediaPlayer.pausePlay();
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        Log.d("AudioFocusChangeListener", "------------------ AUDIOFOCUS_LOSS_TRANSIENT " +
                                "------------------");
//                        if (MyMediaPlayer.currentIndex != -1) {
//                            MyMediaPlayer.instance.pause();
//                            MediaSessionHelper.updatePlaybackState(MyMediaPlayer.mediaSession,
//                                    PlaybackStateCompat.STATE_PAUSED);
//                        }
                        MyMediaPlayer.pausePlay();
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS:
                        Log.d("AudioFocusChangeListener", "------------------ AUDIOFOCUS_LOSS ------------------");
//                        if (MyMediaPlayer.currentIndex != -1) {
//                            MyMediaPlayer.instance.pause();
//                            MediaSessionHelper.updatePlaybackState(MyMediaPlayer.mediaSession,
//                                    PlaybackStateCompat.STATE_PAUSED);
//                        }
                        MyMediaPlayer.pausePlay();
                        break;
                }
            };

    public static AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(getPlaybackAttributes())
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build();
}
