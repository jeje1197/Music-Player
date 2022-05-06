package com.myapp.ceromusicapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView noMusicTextView, minibarTextView, minibarArtistView;
    ArrayList<AudioModel> songList = MyMediaPlayer.getSongList();
    MediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
    int savedPosition;
    SharedPreferences sp;
    RelativeLayout minibarLayout;
    ImageView miniPreviousButton, miniPausePlayButton, miniNextButton;
    static boolean isActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_CeroMusicApp);

        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view);
        noMusicTextView = findViewById(R.id.no_songs_text);
        minibarLayout = findViewById(R.id.minibar_layout);
        minibarTextView = findViewById(R.id.mini_current_song);
        minibarArtistView = findViewById(R.id.mini_current_artist);
        miniPreviousButton = findViewById(R.id.mini_previous);
        miniPausePlayButton = findViewById(R.id.mini_pause_play);
        miniNextButton = findViewById(R.id.mini_next);
        sp = getSharedPreferences("myMusicPlayerSettings", Context.MODE_PRIVATE);
        savedPosition = sp.getInt("savedPosition", 0);
        boolean shuffleOn = sp.getBoolean("shuffleOn", false);
        int repeatMode = sp.getInt("repeatMode", 0);

        ImageView infoButton = findViewById(R.id.informationButton);
        infoButton.setOnClickListener(view -> displayAppInfo());

        setupMinibar();
        setupMinibarButtons();
        //        setShuffleFunctionality(shuffleOn);
        setRepeatFunctionality(repeatMode);

//        Check & request permissions
        if(!checkForPermission()) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1234);
        } else {
            getSongsFromDevice();
        }
    }


    //        Get songs from device
    private void getSongsFromDevice() {
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC");

        while (cursor.moveToNext()) {
            AudioModel songData = new AudioModel(cursor.getString(1), cursor.getString(0),
                    cursor.getString(2), cursor.getString(3));
            if(new File(songData.getPath()).exists()) {
                songList.add(songData);
            }
        }
        cursor.close();

        //        Set up recycler view
        if(songList.size() == 0) {
            noMusicTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new MusicListAdapter(songList, getApplicationContext()));
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    savedPosition = linearLayoutManager.findFirstVisibleItemPosition();
                }
            });
            recyclerView.scrollToPosition(savedPosition);
        }
    }

    boolean checkForPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1234) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getSongsFromDevice();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                Toast.makeText(this, "Read permission is required to fetch songs, please allow from Settings",
                        Toast.LENGTH_SHORT).show();
                noMusicTextView.setVisibility(View.VISIBLE);
            } else {
                noMusicTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    // ------------------------------------

    private void displayAppInfo() {
        MyDialog infoDialog = new MyDialog();
        infoDialog.show(getSupportFragmentManager(), "Information Dialog");
    }

    private void updateRecyclerView() {
        if (recyclerView != null) {
            recyclerView.setAdapter(new MusicListAdapter(songList, getApplicationContext()));
                recyclerView.scrollToPosition(savedPosition);
        }
    }

    private String getCurrentSongTitle() {
        if (MyMediaPlayer.currentIndex == -1) {
            return "No Song Selected";
        }
        return songList.get(MyMediaPlayer.currentIndex).getTitle();
    }

    private String getCurrentSongArtist() {
        if (MyMediaPlayer.currentIndex == -1) {
            return "---";
        }
        return songList.get(MyMediaPlayer.currentIndex).getArtist();
    }

    private void goToCurrentSongView() {
        if (MyMediaPlayer.currentIndex != -1) {
            Intent intent = new Intent(this, MusicPlayerActivity.class);
            startActivity(intent);
        }
    }

    // -------------------
    private void setShuffleFunctionality() {
    }

    //    Sets functionality according to repeatMode
//    0: no repeat 1: repeat all songs 2: repeat current song
    private void setRepeatFunctionality(int repeatMode) {
        switch (repeatMode) {
            case 0:
                mediaPlayer.setOnCompletionListener(mediaPlayer -> MyMediaPlayer.playNextSong());
                return;
            case 1:
                mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                    if (MyMediaPlayer.currentIndex == MyMediaPlayer.songList.size()-1) {
                        MyMediaPlayer.currentIndex = -1;
                    }
                    MyMediaPlayer.playNextSong();
                });
                return;
            case 2:
                mediaPlayer.setOnCompletionListener(mediaPlayer -> MyMediaPlayer.startSong());
        }
    }
    // -------------------

//    Set up mini bar to marquee
    private void setupMinibar() {
            minibarTextView.setHorizontallyScrolling(true);
            minibarTextView.setSelected(true);
    }

    private void setupMinibarButtons() {
//        ------- Thread -------
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && MainActivity.isActive) {
                    if(mediaPlayer.isPlaying()) {
                        miniPausePlayButton.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
                    } else {
                        miniPausePlayButton.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
                    }
                    String currentSongTitle = getCurrentSongTitle();
                    if (minibarTextView.getText() != currentSongTitle) {
                        minibarTextView.setText(currentSongTitle);
                        minibarArtistView.setText(getCurrentSongArtist());
                    }
                }
                new Handler().postDelayed(this, 100);
            }
        });

        miniPreviousButton.setOnClickListener(view -> {
            MyMediaPlayer.playPreviousSong();
            updateRecyclerView();
        });
        miniPausePlayButton.setOnClickListener(view -> MyMediaPlayer.pausePlay());
        miniNextButton.setOnClickListener(view -> {
            MyMediaPlayer.playNextSong();
            updateRecyclerView();
        });
        minibarTextView.setOnClickListener(view -> goToCurrentSongView());
    }

    // --------------------------------------

//    Lifecycle methods
    @Override
    protected void onResume() {
        updateRecyclerView();
        super.onResume();
        isActive = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActive = true;
    }

    @Override
    protected void onPause() {
        isActive = false;
        sp.edit().putInt("savedPosition", savedPosition).apply();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}