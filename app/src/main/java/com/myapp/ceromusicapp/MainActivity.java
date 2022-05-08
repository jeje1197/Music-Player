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
import android.util.Log;
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

import com.myapp.ceromusicapp.Helpers.MediaPlayerHelper;

import java.io.File;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView noMusicTextView, minibarTextView, minibarArtistView;
    ArrayList<AudioModel> deviceSongList = MyMediaPlayer.getOriginalList();
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

//        Load user preferences
//        Shuffle/Repeat states & screen position
        sp = getSharedPreferences("myMusicPlayerSettings", Context.MODE_PRIVATE);
        savedPosition = sp.getInt("savedPosition", 0);
        boolean shuffleOn = sp.getBoolean("shuffleOn", false);
        int repeatMode = sp.getInt("repeatMode", 0);

        MediaPlayerHelper.setShuffleFunctionality(shuffleOn);
        MediaPlayerHelper.setRepeatFunctionality(repeatMode);

        ImageView infoButton = findViewById(R.id.informationButton);
        infoButton.setOnClickListener(view -> displayAppInfo());

//        Check & request permissions
        if(!checkForPermission()) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1234);
        } else {
            getSongsFromDevice();
        }

        setupMinibar();
        setupMinibarButtons();
    }


//    ----------------------------------------------------------------------------------------------
//    Permission Methods here
//    ----------------------------------------------------------------------------------------------

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


//    ----------------------------------------------------------------------------------------------
//    Device Query & RecyclerView Methods here
//    ----------------------------------------------------------------------------------------------

    //  Get songs from device & setup recycler view
    private void getSongsFromDevice() {
        if (deviceSongList.size() > 0) {
            Log.d("-getSongsFromDevice", "Getting songs from device!");
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
                    deviceSongList.add(songData);
                }
            }
            cursor.close();
            MediaPlayerHelper.createShuffledPlaylist(deviceSongList);
        }

        //  Set up recycler view with songs
        if(deviceSongList.size() == 0) {
            noMusicTextView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new MusicListAdapter(deviceSongList, getApplicationContext()));
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

    private void updateRecyclerView() {
        if (recyclerView != null) {
            recyclerView.setAdapter(new MusicListAdapter(deviceSongList, getApplicationContext()));
            recyclerView.scrollToPosition(savedPosition);
        }
    }


//    ----------------------------------------------------------------------------------------------
//    Minibar & Helper Methods here
//    ----------------------------------------------------------------------------------------------

    private String getCurrentSongTitle() {
        if (MyMediaPlayer.currentIndex == -1) {
            return "No Song Selected";
        }
        return MyMediaPlayer.currentSong.getTitle();
    }

    private String getCurrentSongArtist() {
        if (MyMediaPlayer.currentIndex == -1) {
            return "---";
        }
        return MyMediaPlayer.currentSong.getArtist();
    }

    private void goToCurrentSongView() {
        if (MyMediaPlayer.currentIndex != -1) {
            Intent intent = new Intent(this, MusicPlayerActivity.class);
            startActivity(intent);
        }
    }

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
                        updateRecyclerView();
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


//    ----------------------------------------------------------------------------------------------
//    Information Methods here
//    ----------------------------------------------------------------------------------------------

    private void displayAppInfo() {
        MyDialog infoDialog = new MyDialog();
        infoDialog.show(getSupportFragmentManager(), "Information Dialog");
    }


//    ----------------------------------------------------------------------------------------------
//    Lifecycle Methods here
//    ----------------------------------------------------------------------------------------------

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

}