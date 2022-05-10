package com.myapp.ceromusicapp.Helpers;

import android.util.Log;

import com.myapp.ceromusicapp.AudioModel;
import com.myapp.ceromusicapp.MyMediaPlayer;

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
    public static void createShuffledPlaylist(ArrayList<AudioModel> unshuffledList) {
        MyMediaPlayer.shuffledList = new ArrayList<>(unshuffledList);
        Collections.shuffle(MyMediaPlayer.shuffledList);
        Log.d("-createShuffledPlaylist", "Created shuffled list");
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

    public static String convertToMMSS(String duration) {
        long millis = Long.parseLong(duration);
        return String.format(Locale.US, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

}
