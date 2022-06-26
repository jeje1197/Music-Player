package com.myapp.ceromusicapp.Helpers;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;

import com.myapp.ceromusicapp.AudioModel;
import com.myapp.ceromusicapp.MyMediaPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MediaPlayerHelper {

//    ----------------------------------------------------------------------------------------------
//    Shuffle & Repeat Methods here
//    ----------------------------------------------------------------------------------------------

//    Creates a new shuffled list from the parameter
//    and saves it inside of MyMediaPlayer.shuffledList
//    Runs on thread
    public static void createShuffledPlaylist(ArrayList<AudioModel> unshuffledList) {
        new Thread(() -> {
            MyMediaPlayer.shuffledList = new ArrayList<>(unshuffledList);
            Collections.shuffle(MyMediaPlayer.shuffledList);
            Log.d("-createShuffledPlaylist", "Created shuffled list");
        }).start();
    }

//    Sets current playlist = shuffled list if shuffleOn = true
//    Otherwise sets current playlist = deviceSongList
    public static void setShuffleFunctionality(boolean shuffleOn) {
        if (shuffleOn) {
            MyMediaPlayer.currentList = MyMediaPlayer.shuffledList;
        } else {
            MyMediaPlayer.currentList = MyMediaPlayer.originalList;
            MyMediaPlayer.currentIndex = MyMediaPlayer.originalList.indexOf(MyMediaPlayer.currentSong);
        }

        Log.d("-setShuffleFunctionality", "shuffle: " + (shuffleOn ? "on":"off"));
    }

//    Sets how repeat should work
//    0: no repeat, 1: repeat all songs, 2: repeat current song
    public static void setRepeatFunctionality(int repeatMode) {
        switch (repeatMode) {
            case 0:
                MyMediaPlayer.instance.setOnCompletionListener(player -> MyMediaPlayer.playNextSong());
                return;
            case 1:
                MyMediaPlayer.instance.setOnCompletionListener(player -> {
                    if (MyMediaPlayer.currentIndex == MyMediaPlayer.currentList.size()-1) {
                        MyMediaPlayer.currentIndex = -1;
                    }
                    MyMediaPlayer.playNextSong();
                });
                return;
            case 2:
                MyMediaPlayer.instance.setOnCompletionListener(player -> MyMediaPlayer.startSong());
        }
    }

//    ----------------------------------------------------------------------------------------------
//     Methods here
//    ----------------------------------------------------------------------------------------------

    public static boolean checkForLastSavedSong(SharedPreferences sp, ArrayList<AudioModel> songList) {
        if (songList == null) {
            Log.d("-checkForLastSavedSong", "Song List: null");
            return false;
        }
        String last_song_path = sp.getString("last_song_path", null);
        int last_song_position = sp.getInt("last_song_position", -1);

        if (last_song_path != null) {
            for(AudioModel song: songList) {
                if (song.getPath().equals(last_song_path)) {
                    loadSong(song, last_song_path, last_song_position);

                    new Thread(() -> MyMediaPlayer.currentIndex = songList.indexOf(song));
                    return true;
                }
            }
        }

        return false;
    }

    public static void loadSong(AudioModel song, String path, int currentPosition) {
        MyMediaPlayer.initializeMediaPlayer();
        MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
        mediaPlayer.reset();
        MyMediaPlayer.currentSong = song;
        Log.d("-Load Song", "Path: " + path);
        Log.d("-Load Song", "MediaPlayer: " + mediaPlayer);
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.seekTo(currentPosition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveSong(SharedPreferences sp, String path, int position){
        Log.d("Saving Song", String.format("Path: %s\n Current Position: %s\n",
                path, MediaPlayerHelper.convertToMMSS(Integer.toString(position)))
        );
        sp.edit().putString("last_song_path", path)
                .putInt("last_song_position", position)
                .apply();
    }

//    ----------------------------------------------------------------------------------------------
//    Time Format Methods here
//    ----------------------------------------------------------------------------------------------
    public static String convertToMMSS(String duration) {
        long millis = Long.parseLong(duration);
        return String.format(Locale.US, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

}
