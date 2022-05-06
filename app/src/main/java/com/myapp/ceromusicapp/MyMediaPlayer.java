package com.myapp.ceromusicapp;

import static com.myapp.ceromusicapp.MediaSessionHelper.updateMetadata;
import static com.myapp.ceromusicapp.MediaSessionHelper.updatePlaybackState;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;

import java.io.IOException;
import java.util.ArrayList;

public class MyMediaPlayer extends Service {
    protected static MediaPlayer instance;
    protected static ArrayList<AudioModel> songList;
    protected static int currentIndex = -1;
    protected static AudioModel currentSong;
    protected static MediaSessionCompat mediaSession;
    private final String CHANNEL_ID = "channel1";
    private NotificationManager manager;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    On create is only called once during a service's lifecycle so initialize inside of there
    @Override
    public void onCreate() {
        super.onCreate();
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        MediaSessionHelper.createChannel(this, CHANNEL_ID);
        mediaSession = initializeMediaSession(this);
        startSong();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return START_STICKY;
    }

    public static MediaPlayer getInstance() {
        if (instance == null) {
            instance = new MediaPlayer();
        }
        return instance;
    }

    public static ArrayList<AudioModel> getSongList() {
        if (songList == null) {
            songList = new ArrayList<>();
        }
        return songList;
    }

    public static void startSong() {
        currentSong = songList.get(currentIndex);
        instance.reset();
        try {
            instance.setDataSource(currentSong.getPath());
            instance.prepare();
            instance.start();
            updateMetadata(mediaSession, currentSong.getTitle(), currentSong.getArtist());
            updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_PLAYING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void pausePlay() {
        if(MyMediaPlayer.currentIndex == -1)
            return;

        if (instance.isPlaying()) {
            instance.pause();
            updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_PAUSED);
        } else {
            instance.start();
            updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_PLAYING);
        }
    }

    public static void playPreviousSong() {
        if (currentIndex == -1) {
            return;
        }
//        If playback is less than 3sec in, get previous song,
//        otherwise restart current song from beginning
        if (currentIndex > 0 && instance.getCurrentPosition() < 3000 )
            currentIndex--;

        startSong();
    }

    public static void playNextSong() {
        if (currentIndex == -1 || currentIndex == songList.size() - 1)
            return;
        currentIndex++;
        startSong();
    }

//    Notes:
//    - Make sure you initialize the current index within your activity
//    - When doing mutations to songList, create a new variable within
//    your activity using getSongList() and make your changes to that
//    reference so performance is faster
//    - Set your shuffle/repeat functionality within your activity
//    Updates faster and makes buttons look more responsive

    private MediaSessionCompat initializeMediaSession(Context context){
        MediaSessionCompat mediaSession = new MediaSessionCompat(context, "MediaSession Tag");

//        Set callbacks for MediaBroadcastReceiver
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                Log.d("onMediaButtonEvent()", "KeyEvent: "+ keyEvent.toString());
                return super.onMediaButtonEvent(mediaButtonEvent);
            }

            @Override
            public void onSkipToPrevious() {
                playPreviousSong();
                Log.d("onSkipToPrevious()", "Starting Previous Song");
                super.onSkipToPrevious();
            }

            @Override
            public void onPlay() {
                pausePlay();
                Log.d("onPlay()", "Playing song");
                super.onPlay();
            }

            @Override
            public void onPause() {
                pausePlay();
                Log.d("onPause()", "Pausing song");
                super.onPause();
            }

            @Override
            public void onSkipToNext() {
                playNextSong();
                Log.d("onSkipToNext()", "Starting next song");
                super.onSkipToNext();
            }

            @Override
            public void onStop() {
                instance.pause();
                updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_STOPPED);
                Log.d("onStop()", "Notification swiped away");
                super.onStop();
            }
        });

//        Set callback for playback updates
        mediaSession.getController().registerCallback(new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                super.onPlaybackStateChanged(state);

                switch (state.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING:
                        startForeground(1, MediaSessionHelper.getNotification(context, mediaSession,
                                CHANNEL_ID, true).build());
                        Log.d("State_Playing", "Starting foreground");
                        break;

                    case PlaybackStateCompat.STATE_PAUSED:
                        manager.notify(1, MediaSessionHelper.getNotification(context, mediaSession,
                                CHANNEL_ID, false)
                        .build());
                        stopForeground(false);

                        Log.d("State_Paused", "Stopping foreground");
                        break;

                    case PlaybackStateCompat.STATE_STOPPED:
                        Log.d("State_Stopped", "Stopping session");
                        break;

                    case PlaybackStateCompat.STATE_ERROR:
                        Log.d("State_Error", "Error");
                        break;
                }
            }

        });

        mediaSession.setActive(true);
        return mediaSession;
    }

//    Releases all key data objects
    public static void releaseAll() {
        instance.release();
        instance = null;
        mediaSession.setActive(false);
        mediaSession.release();
        mediaSession = null;
    }

    @Override
    public void onDestroy() {
        Log.d("On Destroy: MyMediaPlayer", "Service destroyed");
        releaseAll();
        super.onDestroy();
    }
}