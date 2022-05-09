package com.myapp.ceromusicapp.Helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.myapp.ceromusicapp.MainActivity;
import com.myapp.ceromusicapp.MusicPlayerActivity;
import com.myapp.ceromusicapp.R;

public class MediaSessionHelper {
    private static PlaybackStateCompat.Builder playbackBuilder;
    private static MediaMetadataCompat.Builder metadataBuilder;
    private static final String CHANNEL_ID = "channel1";

//    ----------------------------------------------------------------------------------------------
//    Channel & Notification Methods here
//    ----------------------------------------------------------------------------------------------

//    Creates a notification channel
    public static void createChannel(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "General",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setShowBadge(false);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

//    Create a notification from the data stored in the MediaSession's metadata
    public static NotificationCompat.Builder getNotification(Context context, MediaSessionCompat mediaSession,
                                                             boolean playing) {

        MediaControllerCompat mediaController = mediaSession.getController();
        MediaMetadataCompat metadata = mediaController.getMetadata();
        MediaDescriptionCompat description = metadata.getDescription();

        Intent contentIntent1 = new Intent(context, MainActivity.class);
        Intent contentIntent2 = new Intent(context, MusicPlayerActivity.class);

        PendingIntent contentPendingIntent = PendingIntent.getActivities(context, 0,
                new Intent[]{contentIntent1, contentIntent2}, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Action skipPreviousAction = new NotificationCompat.Action(R.drawable.mini_previous, "Previous",
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        NotificationCompat.Action pausedAction = new NotificationCompat.Action(
                R.drawable.mini_play,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY));

        NotificationCompat.Action playingAction = new NotificationCompat.Action(
                R.drawable.mini_pause,
                "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE));

        NotificationCompat.Action skipNextAction = new NotificationCompat.Action(R.drawable.mini_next, "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

            return new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle(description.getTitle())
                    .setContentText(description.getSubtitle())
                    .setSmallIcon(R.drawable.music_icon_compress)
                    .setSilent(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(contentPendingIntent)
                    .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                            PlaybackStateCompat.ACTION_STOP)
                    )
                    .addAction(skipPreviousAction)
                    .addAction( playing ? playingAction : pausedAction)
                    .addAction(skipNextAction)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 2)
                            .setMediaSession(mediaSession.getSessionToken())
                    );

    }


//    ----------------------------------------------------------------------------------------------
//    Update Metadata & Playback Methods here
//    ----------------------------------------------------------------------------------------------

    public static void updatePlaybackState(MediaSessionCompat mediaSession, int state) {
        if (playbackBuilder == null) {
            playbackBuilder = new PlaybackStateCompat.Builder();
            playbackBuilder.setActions( PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_STOP );
        }

        playbackBuilder.setState(state, 0, 1);
        mediaSession.setPlaybackState(playbackBuilder.build());
    }

    public static void updateMetadata(MediaSessionCompat mediaSession, String title, String artist) {
        if (metadataBuilder == null) {
            metadataBuilder = new MediaMetadataCompat.Builder();
        }
        metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);

        mediaSession.setMetadata(metadataBuilder.build());
    }

}
