package com.myapp.ceromusicapp;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.os.IBinder;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;

import com.myapp.ceromusicapp.Helpers.AudioFocusHelper;
import com.myapp.ceromusicapp.Helpers.MediaPlayerHelper;
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
    static SharedPreferences sp;

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Media Player Service onStart()", "" + intent);
        sp = getSharedPreferences("myMusicPlayerSettings", Context.MODE_PRIVATE);

        if (currentSong == null) MediaPlayerHelper.checkForLastSavedSong(sp, originalList);
        mediaSessionCheck();
        audioManagerCheck();

        startForeground(1, MediaSessionHelper.getNotification(this, mediaSession,
                false));

        if (intent != null) {
            if (intent.hasExtra(START_SONG)) {
                startSong();
            }

            MediaButtonReceiver.handleIntent(mediaSession, intent);
        }

        return START_STICKY;
    }


//    ----------------------------------------------------------------------------------------------
//    MediaPlayer & SongList Getter Methods here
//    ----------------------------------------------------------------------------------------------

    /**
     * @return the current Media Player object
     */
    public static MediaPlayer getInstance() {
        return instance;
    }

    /**
     * Initializes the current MediaPlayer object, if null
     */
    public static void initializeMediaPlayer() {
        if (instance == null)
            instance = new MediaPlayer();
    }

    /**
     * Releases the current MediaPlayer object and sets
     * it to null.
     */
    private static void releaseMediaPlayer() {
        if (instance != null) {
            instance.release();
            instance = null;
        }
    }

    /**
     * Sets the current index and song based on its index in the
     * device song list.
     * @param songIndex
     */
    public static void setCurrentSongFromOriginalList(int songIndex) {
        currentIndex = songIndex;
        currentSong = originalList.get(currentIndex);
    }

    /**
     * Sets the current index and song based on its index in the
     * current song list.
     * @param songIndex
     */
    public static void setCurrentSong(int songIndex) {
        currentIndex = songIndex;
        currentSong = currentList.get(currentIndex);
    }

    /**
     * Sets the current song based on an AudioModel object.
     * Index of current song is automatically updated to match.
     * @param song
     */
    public static void setCurrentSong(AudioModel song) {
        MyMediaPlayer.currentIndex = originalList.indexOf(song);
        currentSong = song;
    }


    /**
     * @return The duration of the song loaded in
     * the Media Player object.
     */
    public static int getMediaPlayerDuration() {
        if (instance != null) {
            return instance.getDuration();
        } else if (MediaPlayerHelper.checkForLastSavedSong(sp, currentList)) {
            return instance.getDuration();
        } else {
            return instance.getDuration();
        }
    }

    /**
     * @return The list of device songs.
     */
    public static ArrayList<AudioModel> getOriginalList() {
        if (originalList == null) {
            originalList = new ArrayList<>();
        }
        return originalList;
    }


//    ----------------------------------------------------------------------------------------------
//    Playback Methods here
//    ----------------------------------------------------------------------------------------------

    /**
     * Starts the current song from playback position 0.
     */
    public static void startSong() {
        initializeMediaPlayer();
        instance.reset();

        try {
            instance.setDataSource(currentSong.getPath());
            instance.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaSessionHelper.updateMetadata(mediaSession, currentSong.getTitle(), currentSong.getArtist());
        if (AudioFocusHelper.isAudioFocusGranted()) {
            instance.start();
            MediaSessionHelper.updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_PLAYING);
        }
    }

    /**
     * Pauses the current song, if playing.
     * Resumes the current song, if paused.
     * If current index is equal to -1, does nothing.
     * @param sp
     */
    public static void pausePlay(SharedPreferences sp) {
        if(MyMediaPlayer.currentIndex == -1) return;

        if (instance != null && instance.isPlaying()) pause(sp);
        else play(sp);
    }

    /**
     * Re-initializes Media Player object, loads data of the
     * current song and resumes playback.
     * If no song is loaded, does nothing.
     * @param sp
     */
    public static void play(SharedPreferences sp) {
        boolean loadedSong = MediaPlayerHelper.checkForLastSavedSong(sp, originalList);

        if (!loadedSong) {
            Log.d("-Play()", "No song loaded");
            return;
        }

        MediaSessionHelper.updateMetadata(mediaSession, currentSong.getTitle(), currentSong.getArtist());
        if (AudioFocusHelper.isAudioFocusGranted()) {
            instance.start();
            MediaSessionHelper.updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_PLAYING);
        }
    }

    /**
     * Pauses the currently playing song, stores its
     * data in SharedPreferences and releases MediaPlayer
     * object.
     * @param sp
     */
    public static void pause(SharedPreferences sp) {
        instance.pause();
        MediaPlayerHelper.saveSong(sp, currentSong.getPath(), instance.getCurrentPosition());
        releaseMediaPlayer();

        MediaSessionHelper.updatePlaybackState(mediaSession, PlaybackStateCompat.STATE_PAUSED);
    }

    /**
     * Decrements the current index and starts song,
     * if playback position is less than 3 seconds in.
     * Otherwise, restarts current song.
     * If current index is -1, does nothing.
     */
    public static void playPreviousSong() {
        if (currentIndex == -1)
            return;

        if (currentIndex > 0 && instance.getCurrentPosition() < 3000 )
            currentIndex--;

        setCurrentSong(currentIndex);
        startSong();
    }

    /**
     * Increments the current index and starts song,
     * if not at end of list.
     * If current index is -1, does nothing.
     */
    public static void playNextSong() {
        if (currentIndex == -1)
            return;

        if (currentIndex < currentList.size() - 1)
            currentIndex++;

        setCurrentSong(currentIndex);
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
                Log.d("-onMediaButtonEvent()------------------", "KeyEvent: "+ keyEvent.toString());
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
                pausePlay(sp);
                Log.d("-onPlay()", "Playing song");
                super.onPlay();
            }

            @Override
            public void onPause() {
                pausePlay(sp);
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
//                instance.pause();
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
                    case PlaybackStateCompat.STATE_CONNECTING:
                        initializeMediaPlayer();
                        mediaSessionCheck();
                        audioManagerCheck();

                        Log.d("-State_Connecting", "Connected!");
                        break;

                    case PlaybackStateCompat.STATE_PLAYING:
                        startForeground(1, MediaSessionHelper.getNotification(context, mediaSession,
                                true));

                        Log.d("-State_Playing", "Starting foreground service");
                        break;

                    case PlaybackStateCompat.STATE_PAUSED:
                        notificationManager.notify(1, MediaSessionHelper.getNotification(context, mediaSession,
                                false));
                        stopForeground(false);

                        Log.d("-State_Paused", "Stopping foreground");
                        break;

                    case PlaybackStateCompat.STATE_STOPPED:
                        stopForeground(true);
//                        stopSelf();
                        Log.d("-State_Stopped", "Stopping session");
                        break;

                    case PlaybackStateCompat.STATE_ERROR:
                        Log.d("-State_Error", "Error");
                        break;
                }
            }

        });

        MediaSessionHelper.updateMetadata(mediaSession,
                currentSong == null ? currentSong.getTitle() : "No Song Selected"
                , currentSong == null ? currentSong.getArtist() : "---");
        MediaSessionHelper.updatePlaybackState(mediaSession, PlaybackState.STATE_PAUSED);

        mediaSession.setActive(true);
        return mediaSession;
    }

    private void mediaSessionCheck() {
        if (mediaSession == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            MediaSessionHelper.createChannel(notificationManager);
            mediaSession = initializeMediaSession(this);
        }
    }

    private static void releaseMediaSession() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
    }

    private void audioManagerCheck() {
        if (AudioFocusHelper.getAudioManager() == null)
            AudioFocusHelper.initializeAudioManager(this);
    }




//    ----------------------------------------------------------------------------------------------
//    Lifecycle & Release Methods here
//    ----------------------------------------------------------------------------------------------

//    Releases all key data objects
//    (audio focus, media session, instance)
    public static void releaseAll() {
        releaseMediaPlayer();
        AudioFocusHelper.releaseAudioManager();
        releaseMediaSession();
    }

//    Release all objects when service is destroyed
    @Override
    public void onDestroy() {
        Log.d("-On Destroy: MyMediaPlayer", "Service destroyed");
        releaseAll();
        super.onDestroy();
    }

}
