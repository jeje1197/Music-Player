package com.myapp.ceromusicapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.myapp.ceromusicapp.Helpers.MediaPlayerHelper;

import java.util.ArrayList;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.ViewHolder>{

    ArrayList<AudioModel> songList;
    int indexOfLastSelected;
    Context context;

    public MusicListAdapter(ArrayList<AudioModel> songsList, Context context) {
        this.songList = songsList;
        this.context = context;
        this.indexOfLastSelected = -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioModel songData = songList.get(position);
        holder.titleTextView.setText(songData.getTitle());
        holder.artistTextView.setText(songData.getArtist());
        holder.durationTextView.setText(MediaPlayerHelper.convertToMMSS(songData.getDuration()));

        if (MyMediaPlayer.currentIndex == holder.getAdapterPosition()) {
            holder.titleTextView.setTextColor(Color.parseColor("#FF0000"));
        } else {
            holder.titleTextView.setTextColor(Color.parseColor("#000000"));
        }

        holder.itemView.setOnClickListener(view -> {
//            Set current song index to clicked adapter position
            MyMediaPlayer.currentIndex = holder.getAdapterPosition();
            context.startService(
                    new Intent(context, MyMediaPlayer.class)
                            .putExtra(MyMediaPlayer.START_SONG, true)
            );

//            Open MusicPlayerActivity
            Intent intent = new Intent(context, MusicPlayerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public int getIndexOfLastSelected() { return indexOfLastSelected; }

    public void updateSelected() {
        if (indexOfLastSelected >= 0)
            notifyItemChanged(indexOfLastSelected);
        notifyItemChanged(MyMediaPlayer.currentIndex);
        indexOfLastSelected = MyMediaPlayer.currentIndex;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        TextView titleTextView, artistTextView, durationTextView;
        ImageView iconImageView;
        public ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.music_title_text);
            iconImageView = itemView.findViewById(R.id.icon_view);
            artistTextView = itemView.findViewById(R.id.music_artist_text);
            durationTextView = itemView.findViewById(R.id.music_duration_text);
        }
    }

}
