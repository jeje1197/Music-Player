package com.myapp.ceromusicapp;

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

import com.myapp.ceromusicapp.Helpers.AudioFocusHelper;
import com.myapp.ceromusicapp.Helpers.MediaSessionHelper;

import java.io.IOException;
import java.util.ArrayList;

public class MyMediaPlayer extends Service {
    public static MediaPlayer instance;
    public static ArrayList<AudioModel> originalList,
            shuffledList, currentList;
    public static int currentIndex = -1;
    public static AudioModel currentSong;
    public static MediaSessionCompat mediaSession;
    private static NotificationManager notificationManager;

    public final static String START_FOREGROUND = "Start Foreground";
    public final static String START_SONG = "Start Song";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    On create is only called once during a service's lifecycle so initialize inside of there
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("-Media Player Service onCreate()",
                "mediaPlayer: " + instance);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        MediaSessionHelper.createChannel(notificationManager);
        mediaSession = initializeMediaSession(this);
        AudioFocusHelper.initializeAudioManager(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            startForeground(1, MediaSessionHelper.getNotification(this, mediaSession,
                    true));
        } else {
            MediaButtonReceiver.handleIntent(mediaSession, intent);
            if (intent.hasExtra(START_SONG)) {
                startSong();
            } else if (intent.hasExtra(START_FOREGROUND)){
                startForeground(1, MediaSessionHelper.getNotification(this, mediaSession,
                        true));
            }
        }

//        Log.d("-Media Player Service onStart()", "" + intent);
//        Log.d("-", String.format(
//                "Media player: %s\n" +
//                        "Current List: %s\n" +
//                        "Current Index: %s\n" +
//                        "Current Song: %s\n" +
//                        "Media Session: %s\n" +
//                        "Notification Manager: %s\n",
//                instance, currentList!=null ? "initialized!":"null", currentIndex, currentSong,
//                mediaSession, notificationManager));

        return START_STICKY;
    }


//    ----------------------------------------------------------------------------------------------
//    MediaPlayer & SongList Getter Methods here
//    ----------------------------------------------------------------------------------------------

    public static MediaPlayer getInstance() {
        if (instance == null) {
            instance = new MediaPlayer();
        }
        return instance;
    }

    public static ArrayList<AudioModel> getOriginalList() {
        if (originalList == null) {
            originalList = new ArrayList<>();
        }
        return originalList;
    }


//    ----------------------------------------------------------------------------------------------
//    Playback Methods here
//    ----------------------------------------------------------------------------------------------

//    Sets current song, updates metadata, requests audio focus,
//    starts the media player and updates playback state.
    public static void startSong() {
//        MediaSessionHelper.updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_CONNECTING);

        currentSong = currentList.get(currentIndex);
        instance.reset();
        try {
            instance.setDataSource(currentSong.getPath());
            instance.prepare();

            MediaSessionHelper.updateMetadata(mediaSession, currentSong.getTitle(), currentSong.getArtist());

            boolean audioFocusGranted = AudioFocusHelper.isAudioFocusGranted();
            Log.d("-startSong()", "Request Granted: "+ audioFocusGranted);
            if (audioFocusGranted) {
                instance.start();
                MediaSessionHelper.updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_PLAYING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    Pauses media player if playing. Starts media player if paused.
//    Updates playback state.
    public static void pausePlay() {
        if(MyMediaPlayer.currentIndex == -1)
            return;

        if (instance.isPlaying()) {
            instance.pause();
            MediaSessionHelper.updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_PAUSED);
        } else {
            boolean audioFocusGranted = AudioFocusHelper.isAudioFocusGranted();
            Log.d("-pausePlay()", "------------------ Request Granted: "+ audioFocusGranted);
            if (audioFocusGranted) {
                instance.start();
                MediaSessionHelper.updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_PLAYING);
            }
        }
    }

//    Decrements the current index if current position >= 3sec
//    and calls startSong(). Otherwise, restarts the current song.
    public static void playPreviousSong() {
        if (currentIndex == -1) {
            return;
        }

        if (currentIndex > 0 && instance.getCurrentPosition() < 3000 )
            currentIndex--;

        startSong();
    }

//    Increments the current index to the next song
//    and calls startSong().
    public static void playNextSong() {
        if (currentIndex == -1)
            return;

        if (currentIndex < currentList.size() - 1)
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


//    ----------------------------------------------------------------------------------------------
//    MediaSession Methods here
//    ----------------------------------------------------------------------------------------------

//    Creates new media session object, sets callbacks for MediaBroadcastReceiver
//    and PlaybackState updates, sets media session to active.
//    Return: media session object
    private MediaSessionCompat initializeMediaSession(Context context){
        MediaSessionCompat mediaSession = new MediaSessionCompat(context, "MediaSession");

//        Set callbacks for MediaBroadcastReceiver
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                Log.d("-onMediaButtonEvent()", "KeyEvent: "+ keyEvent.toString());
                return super.onMediaButtonEvent(mediaButtonEvent);
            }

            @Override
            public void onSkipToPrevious() {
                playPreviousSong();
                Log.d("-onSkipToPrevious()", "Starting Previous Song");
                super.onSkipToPrevious();
            }

            @Override
            public void onPlay() {
                pausePlay();
                Log.d("-onPlay()", "Playing song");
                super.onPlay();
            }

            @Override
            public void onPause() {
                pausePlay();
                Log.d("-onPause()", "Pausing song");
                super.onPause();
            }

            @Override
            public void onSkipToNext() {
                playNextSong();
                Log.d("-onSkipToNext()", "Starting next song");
                super.onSkipToNext();
            }

            @Override
            public void onStop() {
                instance.pause();
                MediaSessionHelper.updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_STOPPED);
                Log.d("-onStop()", "Notification swiped away");
                super.onStop();
            }
        });

//        Set callback for playback state updates
        mediaSession.getController().registerCallback(new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                super.onPlaybackStateChanged(state);

                switch (state.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING:
                        Intent intent = new Intent(getApplicationContext(), MyMediaPlayer.class)
                                .putExtra(START_FOREGROUND, "Start_Foreground");
                        startForegroundService(intent);

                        Log.d("-State_Playing", "Starting foreground service");
                        break;

                    case PlaybackStateCompat.STATE_PAUSED:
                        notificationManager.notify(1, MediaSessionHelper.getNotification(context, mediaSession,
                                false));
                        stopForeground(false);

                        Log.d("-State_Paused", "Stopping foreground");
                        break;

                    case PlaybackStateCompat.STATE_STOPPED:
                        stopForeground(false);
                        stopSelf();
                        Log.d("-State_Stopped", "Stopping session");
                        break;

                    case PlaybackStateCompat.STATE_ERROR:
                        Log.d("-State_Error", "Error");
                        break;
                }
            }

        });

        mediaSession.setActive(true);
        return mediaSession;
    }


//    ----------------------------------------------------------------------------------------------
//    Lifecycle & Release Methods here
//    ----------------------------------------------------------------------------------------------

//    Releases all key data objects
//    (audio focus, media session, instance)
    public static void releaseAll() {
//        AudioFocusHelper.release();
//
//        mediaSession.setActive(false);
//        mediaSession.release();
//        mediaSession = null;

//        instance.release();
//        instance = null;
    }

//    Release all objects when service is destroyed
    @Override
    public void onDestroy() {
        Log.d("-On Destroy: MyMediaPlayer", "Service destroyed");
//        releaseAll();
        super.onDestroy();
//        Log.d("-On Destroy: MyMediaPlayer",
//                String.format(
//                        "Media player: %s\n" +
//                        "Current List: %s\n" +
//                        "Current Index: %s\n" +
//                        "Current Song: %s\n" +
//                        "Current Position: %s\n" +
//                        "Media Session: %s\n" +
//                        "Notification Manager: %s\n",
//                        instance, currentList, currentIndex, currentSong,
//                        instance.getCurrentPosition(), mediaSession, notificationManager)
//        );
    }

}
