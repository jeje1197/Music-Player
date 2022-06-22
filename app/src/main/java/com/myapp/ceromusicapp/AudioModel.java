package com.myapp.ceromusicapp;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AudioModel implements Serializable {
    String path;
    String title;
    String duration;
    String artist;

    public AudioModel(String path, String title, String duration, String artist) {
        this.path = path;
        this.title = title;
        this.duration = duration;
        this.artist = artist;
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        long millis = Long.parseLong(duration);
        return String.format(Locale.US, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

    public String getArtist() {
        if (artist.equals("<unknown>")) {
            return "Unknown artist";
        }
        return artist;
    }

}
