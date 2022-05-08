package com.myapp.ceromusicapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.myapp.ceromusicapp.Helpers.MediaPlayerHelper;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    private TextView titleView, artistView, currentTimeView, totalTimeView;
    private SeekBar seekbar;
    private ImageView pausePlayButton;
    private ImageView musicIcon;
    private ImageView shuffleButton;
    private ImageView repeatButton;
    private final MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
    private AudioModel currentSong;
    private int degrees = 0;
    private boolean shuffleOn;
    private int repeatMode = 0;
    private SharedPreferences sp;
    private static boolean isActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        titleView = findViewById(R.id.song_title);
        artistView = findViewById(R.id.song_artist);
        currentTimeView = findViewById(R.id.current_time);
        totalTimeView = findViewById(R.id.total_time);
        seekbar = findViewById(R.id.seek_bar);
        ImageView previousButton = findViewById(R.id.previous);
        pausePlayButton = findViewById(R.id.pause_play);
        ImageView nextButton = findViewById(R.id.next);
        musicIcon = findViewById(R.id.music_icon_big);
        shuffleButton = findViewById(R.id.shuffle);
        repeatButton = findViewById(R.id.repeat);

//        Set up marquee
        titleView.setHorizontallyScrolling(true);
        titleView.setSelected(true);

//        Update the view for the current song (title, progress bar)
        setResourcesWithMusic();
        loadShuffleAndRepeatStates();

        MusicPlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && MusicPlayerActivity.isActive) {
                    if (currentSong != MyMediaPlayer.currentSong){
                        setResourcesWithMusic();
                    }

                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekbar.setProgress(currentPosition);
                    currentTimeView.setText(convertToMMSS(currentPosition + ""));

                    if(mediaPlayer.isPlaying()) {
                        pausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
                        musicIcon.setRotation(degrees++);
                    } else {
                        pausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
                    }
                }
                new Handler().postDelayed(this, 100);
            }
        });

        previousButton.setOnClickListener(view -> MyMediaPlayer.playPreviousSong());
        pausePlayButton.setOnClickListener(view -> MyMediaPlayer.pausePlay());
        nextButton.setOnClickListener(view -> MyMediaPlayer.playNextSong());
        shuffleButton.setOnClickListener(view -> toggleShuffle());
        repeatButton.setOnClickListener(view -> toggleRepeat());
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (mediaPlayer != null && b) {
                    mediaPlayer.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

    }


//    ----------------------------------------------------------------------------------------------
//    Update View Methods here
//    ----------------------------------------------------------------------------------------------

    void setResourcesWithMusic() {
        currentSong = MyMediaPlayer.currentSong;
        titleView.setText(currentSong.getTitle());
        artistView.setText(currentSong.getArtist());
        totalTimeView.setText(convertToMMSS(currentSong.getDuration()));
        seekbar.setMax(mediaPlayer.getDuration());
    }

    public static String convertToMMSS(String duration) {
        long millis = Long.parseLong(duration);
        return String.format(Locale.US, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }


//    ----------------------------------------------------------------------------------------------
//    Shuffle & Repeat Methods here
//    ----------------------------------------------------------------------------------------------

    private void loadShuffleAndRepeatStates() {
        sp = getSharedPreferences("myMusicPlayerSettings", Context.MODE_PRIVATE);
        shuffleOn = sp.getBoolean("shuffleOn", false);
        repeatMode = sp.getInt("repeatMode", 0);

        if(shuffleOn)
            shuffleButton.setImageResource(R.drawable.ic_baseline_shuffle_purple_24);

        switch (repeatMode) {
            case 1:
                repeatButton.setImageResource(R.drawable.ic_baseline_repeat_purple_24);
                break;
            case 2:
                repeatButton.setImageResource(R.drawable.ic_baseline_repeat_one_purple_24);
                break;
        }
    }

//    Toggle between shuffle off & shuffle on mode
    private void toggleShuffle() {
        if (!shuffleOn) {
            shuffleButton.setImageResource(R.drawable.ic_baseline_shuffle_purple_24);
            shuffleOn = true;
        } else {
            shuffleButton.setImageResource(R.drawable.ic_baseline_shuffle_24);
            shuffleOn = false;
        }
        MediaPlayerHelper.setShuffleFunctionality(shuffleOn);
    }

//    Toggle between repeat off, repeat all, and repeat one modes
    private void toggleRepeat() {
        switch (repeatMode) {
            case 0:
                repeatButton.setImageResource(R.drawable.ic_baseline_repeat_purple_24);
                repeatMode = 1;
                break;
            case 1:
                repeatButton.setImageResource(R.drawable.ic_baseline_repeat_one_purple_24);
                repeatMode = 2;
                break;
            case 2:
                repeatButton.setImageResource(R.drawable.ic_baseline_repeat_24);
                repeatMode = 0;
        }
        MediaPlayerHelper.setRepeatFunctionality(repeatMode);
    }


//    ----------------------------------------------------------------------------------------------
//    Lifecycle Methods here
//    ----------------------------------------------------------------------------------------------
    @Override
    protected void onStart() {
        isActive = true;
        super.onStart();
    }

    @Override
    protected void onPause() {
        isActive = false;
        sp.edit().putBoolean("shuffleOn", shuffleOn)
                .putInt("repeatMode", repeatMode)
                .apply();
        super.onPause();
    }

    @Override
    protected void onResume() {
        isActive = true;
        super.onResume();
    }

}
