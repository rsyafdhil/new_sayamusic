package com.example.sayamusicc;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.sayamusicc.network.JamendoApi;
import com.example.sayamusicc.network.RetrofitInstance;
import com.example.sayamusicc.network.models.TrackResponse;

public class SearchSongActivity extends AppCompatActivity {
    private RecyclerView rvSearchResults;
    private SearchResultsAdapter adapter;
    private ExoPlayer player;
    private TextView tvNowPlaying, tvSearchResults;
    private Button btnPause, btnStop, btnPlay, btnSearch, btnClearSearch;
    private EditText etSearch;
    private RadioGroup rgSearchType;
    private BottomNavigationView bottomNavigation;

    private Track currentTrack;
    private List<Track> searchResults = new ArrayList<>();
    String clientId = "c9993a62";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_song);

        initializeViews();
        setupRecyclerView();
        setupPlayer();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initializeViews() {
        rvSearchResults = findViewById(R.id.rvSearchResults);
        tvNowPlaying = findViewById(R.id.tvNowPlaying);
        tvSearchResults = findViewById(R.id.tvSearchResults);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnPlay = findViewById(R.id.btnPlay);
        btnSearch = findViewById(R.id.btnSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        etSearch = findViewById(R.id.etSearch);
        rgSearchType = findViewById(R.id.rgSearchType);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupRecyclerView() {
        adapter = new SearchResultsAdapter(new ArrayList<>());
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v -> {
            if (currentTrack != null) {
                String audioUrl = getAudioUrl(currentTrack);
                playUrl(audioUrl, currentTrack.getName() + " - " + currentTrack.getArtistName());
            } else {
                Toast.makeText(this, "No track selected", Toast.LENGTH_SHORT).show();
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
        });

        btnSearch.setOnClickListener(v -> performSearch());

        btnClearSearch.setOnClickListener(v -> clearSearch());

        // Search when Enter is pressed
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_search);

        bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(SearchSongActivity.this, MainActivity.class));
                    overridePendingTransition(0, 0);
                    finish(); // Close current activity
                    return true;
                } else if (itemId == R.id.nav_search) {
                    // Already on search
                    return true;
                }
                return false;
            }
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();

        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Enter search keyword", Toast.LENGTH_SHORT).show();
            return;
        }

        if (query.length() < 2) {
            Toast.makeText(this, "Keyword must be at least 2 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        tvNowPlaying.setText("🔍 Searching for: " + query + "...");
        tvSearchResults.setText("Searching...");
        btnSearch.setEnabled(false);
        btnSearch.setText("🔄 Searching...");

        JamendoApi api = RetrofitInstance.getApi();
        String clientId = BuildConfig.JAMENDO_CLIENT_ID;

        Call<TrackResponse> call;

        // Determine search type based on radio button selection
        int checkedId = rgSearchType.getCheckedRadioButtonId();

        if (checkedId == R.id.rbArtist) {
            call = api.searchTracksByArtist(clientId, "json", 30, query, "popularity_total");
            Log.d("Search", "Searching by artist: " + query);
        } else if (checkedId == R.id.rbTrack) {
            call = api.searchTracksByName(clientId, "json", 30, query, "popularity_total");
            Log.d("Search", "Searching by track name: " + query);
        } else {
            call = api.searchTracks(clientId, "json", 30, query, "popularity_total");
            Log.d("Search", "General search: " + query);
        }

        call.enqueue(new Callback<TrackResponse>() {
            @Override
            public void onResponse(Call<TrackResponse> call, Response<TrackResponse> response) {
                btnSearch.setEnabled(true);
                btnSearch.setText("🔍 Search");

                if (response.isSuccessful() && response.body() != null) {
                    List<Track> results = response.body().getResults();
                    Log.d("Search", "Found " + results.size() + " tracks");

                    if (results.isEmpty()) {
                        Toast.makeText(SearchSongActivity.this, "No results for: " + query,
                                Toast.LENGTH_SHORT).show();
                        tvNowPlaying.setText("No results found for: " + query);
                        tvSearchResults.setText("No results found. Try different keywords.");
                    } else {
                        searchResults.clear();
                        searchResults.addAll(results);
                        adapter.setTracks(results);

                        btnClearSearch.setVisibility(View.VISIBLE);

                        // Set first result as current track
                        currentTrack = results.get(0);
                        tvNowPlaying.setText("🎵 Found " + results.size() + " tracks. Ready: " +
                                currentTrack.getName() + " by " + currentTrack.getArtistName());
                        tvSearchResults.setText("🎉 Search Results (" + results.size() + " tracks)");
                    }
                } else {
                    Log.e("Search", "Search failed: " + response.code());
                    Toast.makeText(SearchSongActivity.this, "Search failed, try again",
                            Toast.LENGTH_SHORT).show();
                    tvNowPlaying.setText("Search failed");
                    tvSearchResults.setText("Search failed. Check your connection.");
                }
            }

            @Override
            public void onFailure(Call<TrackResponse> call, Throwable t) {
                btnSearch.setEnabled(true);
                btnSearch.setText("🔍 Search");
                Log.e("Search", "Search error", t);
                Toast.makeText(SearchSongActivity.this, "Error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                tvNowPlaying.setText("Search error");
                tvSearchResults.setText("Network error occurred.");
            }
        });
    }

    private void clearSearch() {
        etSearch.setText("");
        btnClearSearch.setVisibility(View.GONE);

        // Clear results
        searchResults.clear();
        adapter.setTracks(new ArrayList<>());

        currentTrack = null;
        tvNowPlaying.setText("Search cleared");
        tvSearchResults.setText("Search for music above");

        Toast.makeText(this, "Search cleared", Toast.LENGTH_SHORT).show();
    }

    private String getAudioUrl(Track track) {
        String audioUrl = track.getAudio();
        if (audioUrl == null || audioUrl.isEmpty()) {
            audioUrl = "https://api.jamendo.com/v3.0/tracks/file/?client_id=" +
                    BuildConfig.JAMENDO_CLIENT_ID + "&id=" + track.getId() + "&action=stream";
        }
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

    // SearchResultsAdapter
    private class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.SearchViewHolder> {
        private List<Track> tracks;

        SearchResultsAdapter(List<Track> tracks) {
            this.tracks = tracks;
        }

        void setTracks(List<Track> newTracks) {
            this.tracks = newTracks;
            runOnUiThread(() -> notifyDataSetChanged());
        }

        @Override
        public SearchViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_track, parent, false);
            return new SearchViewHolder(v);
        }

        @Override
        public void onBindViewHolder(SearchViewHolder holder, int position) {
            Track t = tracks.get(position);
            holder.tvTitle.setText(t.getName());
            holder.tvArtist.setText(t.getArtistName() != null ? t.getArtistName() : "Unknown Artist");

            Glide.with(SearchSongActivity.this)
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

        class SearchViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView imgCover;
            android.widget.TextView tvTitle, tvArtist;
            android.widget.Button btnPlay;

            SearchViewHolder(View itemView) {
                super(itemView);
                imgCover = itemView.findViewById(R.id.imgCover);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvArtist = itemView.findViewById(R.id.tvArtist);
                btnPlay = itemView.findViewById(R.id.btnPlay);
            }
        }
    }
}