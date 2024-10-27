package com.anthonybturner.cinemapostersanywhere.adapters;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anthonybturner.cinemapostersanywhere.Models.Video;
import com.anthonybturner.cinemapostersanywhere.R;
import com.bumptech.glide.Glide;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final List<Video> videoList;
    private final OnVideoClickListener listener;
    public int selectedPosition = -1; // Variable to track the selected position
    public int focusedPosition = -1;   // Variable to track the focused position for navigation

    public interface OnVideoClickListener {
        void onVideoClick(String videoId);
    }

    public VideoAdapter(List<Video> videoList, OnVideoClickListener listener) {
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item, parent, false);
        return new VideoViewHolder(view, this); // Pass 'this' to the ViewHolder
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videoList.get(position);
        holder.bind(video, listener, position == selectedPosition, position == focusedPosition); // Pass selected and focused states
        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition(); // Update the selected position
            notifyDataSetChanged(); // Refresh the list to reflect changes
            listener.onVideoClick(video.getVideoId()); // Notify listener for video click
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFocusedPosition(int position) {
        focusedPosition = position; // Update the focused position
        notifyDataSetChanged(); // Refresh the list to reflect changes
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView title;
        public VideoViewHolder(@NonNull View itemView, VideoAdapter adapter) {
            super(itemView);
            // Keep a reference to the adapter
            title = itemView.findViewById(R.id.video_title);
            thumbnail = itemView.findViewById(R.id.video_thumbnail);
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    adapter.focusedPosition = getAdapterPosition(); // Update focused position
                    title.setSelected(true); // Start marquee when item is focused
                    title.requestFocus(); // Request focus on the title to ensure marquee works
                   itemView.setBackgroundResource(R.drawable.video_item_background_highlight);
                } else {
                   itemView.setBackgroundResource(R.drawable.video_item_background); // Revert to default background
                    title.setSelected(false); // Stop marquee when focus is lost
                }
            });
        }
        public void bind(Video video, OnVideoClickListener listener, boolean isSelected, boolean isFocused) {
            title.setText(video.getTitle());
            Glide.with(thumbnail.getContext()).load(video.getThumbnailUrl()).into(thumbnail);

        }
    }
}