package com.example.sayamusicc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
import com.example.sayamusicc.network.JamendoApi;
import com.example.sayamusicc.network.RetrofitInstance;
import com.example.sayamusicc.network.models.TrackResponse;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvTracks;
    private TracksAdapter adapter;
    private ExoPlayer player;
    private TextView tvNowPlaying;
    private Button btnPause, btnStop, btnPlay, btnShuffle;
    private BottomNavigationView bottomNavigation;

    private Track currentTrack;
    private List<Track> allTracks = new ArrayList<>();
    private Random random = new Random();
    private boolean isShuffleMode = true; // Default shuffle ON
    String clientId = "c9993a62";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide Action Bar if exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        setupRecyclerView();
        setupPlayer();
        setupClickListeners();
        setupBottomNavigation();
        loadTracks();
    }

    private void initializeViews() {
        rvTracks = findViewById(R.id.rvTracks);
        tvNowPlaying = findViewById(R.id.tvNowPlaying);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnPlay = findViewById(R.id.btnPlay);
        btnShuffle = findViewById(R.id.btnShuffle);
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Update shuffle button
        updateShuffleButton();
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
            if (currentTrack == null || isShuffleMode) {
                playRandomTrack();
            } else {
                String audioUrl = getAudioUrl(currentTrack);
                playUrl(audioUrl, currentTrack.getName() + " - " + currentTrack.getArtistName());
            }
        });

        btnPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                btnPause.setText("⏯ Resume");
            } else {
                player.play();
                btnPause.setText("⏸ Pause");
            }
        });

        btnStop.setOnClickListener(v -> {
            player.stop();
            tvNowPlaying.setText("Stopped");
            btnPause.setText("⏸ Pause");

            if (isShuffleMode) {
                selectRandomTrack();
            }
        });

        btnShuffle.setOnClickListener(v -> {
            isShuffleMode = !isShuffleMode;
            updateShuffleButton();
            Toast.makeText(this, "Shuffle " + (isShuffleMode ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // Already on home
                    return true;
                } else if (itemId == R.id.nav_search) {
                    startActivity(new Intent(MainActivity.this, SearchSongActivity.class));
                    overridePendingTransition(0, 0); // No animation
                    return true;
                }
                return false;
            }
        });
    }

    private void updateShuffleButton() {
        btnShuffle.setText(isShuffleMode ? "🔀 Shuffle: ON" : "🔀 Shuffle: OFF");
        btnShuffle.setBackgroundColor(isShuffleMode ?
                getResources().getColor(android.R.color.holo_green_light) :
                getResources().getColor(android.R.color.darker_gray));
    }

    private void playRandomTrack() {
        if (allTracks.isEmpty()) {
            Toast.makeText(this, "No tracks available", Toast.LENGTH_SHORT).show();
            return;
        }

        Track randomTrack = selectRandomTrack();
        String audioUrl = getAudioUrl(randomTrack);
        playUrl(audioUrl, randomTrack.getName() + " - " + randomTrack.getArtistName());
    }

    private Track selectRandomTrack() {
        if (allTracks.isEmpty()) return null;

        int randomIndex = random.nextInt(allTracks.size());
        currentTrack = allTracks.get(randomIndex);

        Log.d("RandomTrack", "Selected: " + currentTrack.getName() +
                " by " + currentTrack.getArtistName());

        return currentTrack;
    }

    private String getAudioUrl(Track track) {
        String audioUrl = track.getAudio();
        if (audioUrl == null || audioUrl.isEmpty()) {
            audioUrl = "https://api.jamendo.com/v3.0/tracks/file/?client_id=" +
                    BuildConfig.JAMENDO_CLIENT_ID + "&id=" + track.getId() + "&action=stream";
        }
        return audioUrl;
    }

    private void loadTracks() {
        JamendoApi api = RetrofitInstance.getApi();
        String clientId = BuildConfig.JAMENDO_CLIENT_ID;

        Call<TrackResponse> call = api.getTracks(clientId, "json", 50, "popularity_total");

        call.enqueue(new Callback<TrackResponse>() {
            @Override
            public void onResponse(Call<TrackResponse> call, Response<TrackResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Track> tracks = response.body().getResults();
                    Log.d("API_RESPONSE", "Loaded " + tracks.size() + " tracks");

                    allTracks.clear();
                    allTracks.addAll(tracks);
                    adapter.setTracks(tracks);

                    if (!tracks.isEmpty()) {
                        currentTrack = selectRandomTrack();
                        tvNowPlaying.setText("Ready: " + currentTrack.getName() +
                                " - " + currentTrack.getArtistName());
                    }
                } else {
                    Log.e("MainActivity", "API error: " + response.code());
                    Toast.makeText(MainActivity.this, "Failed to load tracks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TrackResponse> call, Throwable t) {
                Log.e("MainActivity", "API failure", t);
                Toast.makeText(MainActivity.this, "Network Error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
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
            tvNowPlaying.setText("🎵 Now Playing: " + title);
            btnPause.setText("⏸ Pause");

            Log.d("PlayUrl", "Playing: " + title);
        } catch (Exception e) {
            Log.e("PlayUrl", "Error playing track", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }

    // TracksAdapter
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

            Glide.with(MainActivity.this)
                    .load(t.getAlbumImage())
                    .placeholder(android.R.drawable.ic_media_play)
                    .into(holder.imgCover);

            holder.btnPlay.setOnClickListener(v -> {
                currentTrack = t;
                String audioUrl = getAudioUrl(t);
                playUrl(audioUrl, t.getName() + " — " + t.getArtistName());
            });
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