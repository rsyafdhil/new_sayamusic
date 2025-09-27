package com.example.sayamusicc;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sayamusicc.BuildConfig;
import com.example.sayamusicc.network.models.Track;
import com.bumptech.glide.Glide;

import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.example.sayamusicc.network.JamendoApi;
import com.example.sayamusicc.network.RetrofitInstance;
import com.example.sayamusicc.network.models.Track;
import com.example.sayamusicc.network.models.TrackResponse;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvTracks;
    private TracksAdapter adapter;
    private ExoPlayer player;
    private TextView tvNowPlaying;
    private Button btnPause, btnStop, btnPlay, btnNext, btnPrev;
    private Track currentTrack;
    private List<Track> allTracks = new ArrayList<>();
    private int currentTrackIndex = 0;
    private Random random = new Random();
    String clientId = "c9993a62";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        setupPlayer();
        setupClickListeners();
        loadTracks();
    }

    private void initializeViews() {
        rvTracks = findViewById(R.id.rvTracks);
        tvNowPlaying = findViewById(R.id.tvNowPlaying);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnPlay = findViewById(R.id.btnPlay);
        // Optional: Add these buttons to your layout if you want navigation
        // btnNext = findViewById(R.id.btnNext);
        // btnPrev = findViewById(R.id.btnPrev);
    }

    private void setupRecyclerView() {
        adapter = new TracksAdapter(new ArrayList<>());
        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(adapter);
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v -> {
            if (allTracks.isEmpty()) {
                Toast.makeText(this, "No tracks available. Loading...", Toast.LENGTH_SHORT).show();
                loadTracks();
                return;
            }

            if (currentTrack != null) {
                // Resume current track
                resumeCurrentTrack();
            } else {
                // Play first track or random track
                playTrackAtIndex(0);
            }
        });

        btnPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                btnPause.setText("Resume");
            } else {
                player.play();
                btnPause.setText("Pause");
            }
        });

        btnStop.setOnClickListener(v -> {
            player.stop();
            tvNowPlaying.setText("Stopped");
            btnPause.setText("Pause");
        });

        // Optional navigation buttons
        /*
        btnNext.setOnClickListener(v -> playNextTrack());
        btnPrev.setOnClickListener(v -> playPreviousTrack());
        */
    }

    private void loadTracks() {
        tvNowPlaying.setText("Loading tracks...");

        // Use fallback client ID if BuildConfig is null
        String apiClientId = (BuildConfig.JAMENDO_CLIENT_ID != null && !BuildConfig.JAMENDO_CLIENT_ID.isEmpty())
                ? BuildConfig.JAMENDO_CLIENT_ID
                : clientId;

        Log.d("API_DEBUG", "Using client ID: " + apiClientId);

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.jamendo.com/v3.0/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        JamendoApi api = retrofit.create(JamendoApi.class);

        // Load multiple pages of tracks for variety
        loadTracksPage(api, apiClientId, 0, 50); // Load first 50 tracks
    }

    private void loadTracksPage(JamendoApi api, String apiClientId, int offset, int limit) {
        Call<TrackResponse> call = api.getTracks(apiClientId, "json", limit, "popularity_total");

        call.enqueue(new Callback<TrackResponse>() {
            @Override
            public void onResponse(Call<TrackResponse> call, Response<TrackResponse> response) {
                Log.d("API_DEBUG", "Response code: " + response.code());
                Log.d("API_DEBUG", "Request URL: " + call.request().url().toString());

                if (response.isSuccessful() && response.body() != null) {
                    List<Track> tracks = response.body().getResults();
                    Log.d("API_RESPONSE", "Loaded " + (tracks != null ? tracks.size() : 0) + " tracks");

                    if (tracks != null && !tracks.isEmpty()) {
                        // Add tracks to main list
                        allTracks.addAll(tracks);

                        // Update adapter
                        adapter.setTracks(new ArrayList<>(allTracks));

                        // Set first track as current if none selected
                        if (currentTrack == null && !allTracks.isEmpty()) {
                            currentTrack = allTracks.get(0);
                            currentTrackIndex = 0;
                            tvNowPlaying.setText("Ready: " + currentTrack.getName() +
                                    " - " + currentTrack.getArtistName());
                        }

                        // Log some tracks for debugging
                        for (int i = 0; i < Math.min(3, tracks.size()); i++) {
                            Track t = tracks.get(i);
                            Log.d("TRACK_INFO", "Track " + i + ": " + t.getName()
                                    + " | Artist: " + t.getArtistName()
                                    + " | Audio: " + t.getAudio());
                        }

                        Toast.makeText(MainActivity.this,
                                "✅ Loaded " + allTracks.size() + " tracks total!",
                                Toast.LENGTH_SHORT).show();

                        // Load more tracks for variety (optional)
                        if (allTracks.size() < 100) {
                            loadMoreTracksWithDifferentGenres(api, apiClientId);
                        }
                    } else {
                        Log.e("API_ERROR", "No tracks in response");
                        tvNowPlaying.setText("No tracks found");
                        Toast.makeText(MainActivity.this, "❌ No tracks found", Toast.LENGTH_LONG).show();
                    }
                } else {
                    String error = "HTTP " + response.code();
                    Log.e("API_ERROR", error);
                    tvNowPlaying.setText("API Error");
                    Toast.makeText(MainActivity.this, "❌ API Error: " + error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TrackResponse> call, Throwable t) {
                Log.e("API_FAILURE", "Network error", t);
                tvNowPlaying.setText("Network error");
                Toast.makeText(MainActivity.this, "❌ Network Error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadMoreTracksWithDifferentGenres(JamendoApi api, String apiClientId) {
        // Load tracks with different sorting for variety
        String[] sortTypes = {"releasedate_desc", "duration_desc", "name_asc"};

        for (String sortType : sortTypes) {
            Call<TrackResponse> call = api.getTracks(apiClientId, "json", 20, sortType);

            call.enqueue(new Callback<TrackResponse>() {
                @Override
                public void onResponse(Call<TrackResponse> call, Response<TrackResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Track> moreTracks = response.body().getResults();
                        if (moreTracks != null && !moreTracks.isEmpty()) {
                            allTracks.addAll(moreTracks);
                            adapter.setTracks(new ArrayList<>(allTracks));
                            Log.d("MORE_TRACKS", "Total tracks now: " + allTracks.size());
                        }
                    }
                }

                @Override
                public void onFailure(Call<TrackResponse> call, Throwable t) {
                    Log.e("MORE_TRACKS", "Failed to load more tracks", t);
                }
            });
        }
    }

    private void playTrackAtIndex(int index) {
        if (allTracks.isEmpty() || index < 0 || index >= allTracks.size()) {
            Toast.makeText(this, "Invalid track index", Toast.LENGTH_SHORT).show();
            return;
        }

        currentTrack = allTracks.get(index);
        currentTrackIndex = index;

        String audioUrl = getAudioUrl(currentTrack);
        playUrl(audioUrl, currentTrack.getName() + " - " + currentTrack.getArtistName());
    }

    private void resumeCurrentTrack() {
        if (currentTrack != null) {
            String audioUrl = getAudioUrl(currentTrack);
            playUrl(audioUrl, currentTrack.getName() + " - " + currentTrack.getArtistName());
        }
    }

    private void playNextTrack() {
        if (allTracks.isEmpty()) return;

        currentTrackIndex = (currentTrackIndex + 1) % allTracks.size();
        playTrackAtIndex(currentTrackIndex);
    }

    private void playPreviousTrack() {
        if (allTracks.isEmpty()) return;

        currentTrackIndex = (currentTrackIndex - 1 + allTracks.size()) % allTracks.size();
        playTrackAtIndex(currentTrackIndex);
    }

    private void playRandomTrack() {
        if (allTracks.isEmpty()) return;

        int randomIndex = random.nextInt(allTracks.size());
        playTrackAtIndex(randomIndex);
    }

    private String getAudioUrl(Track track) {
        String audioUrl = track.getAudio();

        if (audioUrl == null || audioUrl.isEmpty()) {
            String apiClientId = (BuildConfig.JAMENDO_CLIENT_ID != null && !BuildConfig.JAMENDO_CLIENT_ID.isEmpty())
                    ? BuildConfig.JAMENDO_CLIENT_ID
                    : clientId;

            audioUrl = "https://api.jamendo.com/v3.0/tracks/file/?client_id=" +
                    apiClientId + "&id=" + track.getId() + "&action=stream";
        }

        Log.d("AUDIO_URL", "Using URL: " + audioUrl);
        return audioUrl;
    }

    private void playUrl(String url, String title) {
        if (url == null || url.isEmpty()) {
            tvNowPlaying.setText("No audio URL");
            return;
        }

        try {
            MediaItem mediaItem = MediaItem.fromUri(url);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();

            tvNowPlaying.setText("Now Playing: " + title);
            btnPause.setText("Pause");

            Log.d("PLAY_URL", "Playing: " + title + " from URL: " + url);
        } catch (Exception e) {
            Log.e("PLAY_URL", "Error playing track", e);
            Toast.makeText(this, "Error playing track: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    // Adapter (inner class)
    private class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.TrackViewHolder> {
        private List<Track> tracks;

        TracksAdapter(List<Track> tracks) {
            this.tracks = tracks;
        }

        void setTracks(List<Track> newTracks) {
            this.tracks = newTracks;
            runOnUiThread(() -> notifyDataSetChanged());
        }

        @Override
        public TrackViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_track, parent, false);
            return new TrackViewHolder(v);
        }

        @Override
        public void onBindViewHolder(TrackViewHolder holder, int position) {
            Track t = tracks.get(position);
            holder.tvTitle.setText(t.getName());
            holder.tvArtist.setText(t.getArtistName() != null ? t.getArtistName() : "Unknown Artist");

            // Load album image
            Glide.with(MainActivity.this)
                    .load(t.getAlbumImage())
                    .placeholder(android.R.drawable.ic_media_play)
                    .error(android.R.drawable.ic_media_play)
                    .into(holder.imgCover);

            // Set play button click listener
            holder.btnPlay.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < tracks.size()) {
                    Track selectedTrack = tracks.get(adapterPosition);

                    // Update current track and index
                    currentTrack = selectedTrack;
                    currentTrackIndex = adapterPosition;

                    // Play the selected track
                    String audioUrl = getAudioUrl(selectedTrack);
                    playUrl(audioUrl, selectedTrack.getName() + " — " + selectedTrack.getArtistName());

                    // Show feedback
                    Toast.makeText(MainActivity.this,
                            "🎵 Playing: " + selectedTrack.getName(),
                            Toast.LENGTH_SHORT).show();

                    // Refresh the adapter to update highlighting
                    notifyDataSetChanged();
                }
            });

            // Highlight currently playing track
            if (currentTrack != null && t.getId().equals(currentTrack.getId())) {
                holder.itemView.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            } else {
                holder.itemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }
        }

        @Override
        public int getItemCount() {
            return tracks == null ? 0 : tracks.size();
        }

        class TrackViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView imgCover;
            android.widget.TextView tvTitle, tvArtist;
            android.widget.Button btnPlay;

            TrackViewHolder(View itemView) {
                super(itemView);
                imgCover = itemView.findViewById(R.id.imgCover);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvArtist = itemView.findViewById(R.id.tvArtist);
                btnPlay = itemView.findViewById(R.id.btnPlay);
            }
        }
    }
}