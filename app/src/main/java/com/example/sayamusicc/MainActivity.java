package com.example.sayamusicc;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.sayamusicc.network.JamendoApi;
import com.example.sayamusicc.network.RetrofitInstance;
import com.example.sayamusicc.network.models.Track;
import com.example.sayamusicc.network.models.TrackResponse;



class Artist {
    String name;
}

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvTracks;
    private TracksAdapter adapter;
    private ExoPlayer player;
    private TextView tvNowPlaying;
    private Button btnPause, btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvTracks = findViewById(R.id.rvTracks);
        tvNowPlaying = findViewById(R.id.tvNowPlaying);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);

        adapter = new TracksAdapter(new ArrayList<>());
        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(adapter);

        player = new ExoPlayer.Builder(this).build();

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
            tvNowPlaying.setText("Not playing");
            btnPause.setText("Pause");
        });

        loadTracks();
    }

    private void loadTracks(){
        JamendoApi api = RetrofitInstance.getApi();
        String clientId = BuildConfig.JAMENDO_CLIENT_ID;
        Call<com.example.sayamusicc.network.models.TrackResponse> call = api.getTracks(clientId, "json", 30, "mp32", "id,name,artist_name,audio,album_image");
        call.enqueue(new Callback<com.example.sayamusicc.network.models.TrackResponse>() {
            @Override
            public void onResponse(Call<com.example.sayamusicc.network.models.TrackResponse> call, Response<com.example.sayamusicc.network.models.TrackResponse> response) {
                if (response.isSuccessful() && response.body() != null){
                    List<Track> tracks = response.body().getResults();
                    adapter.setTracks(tracks);
                } else {
                    Log.e("MainActivity", "API empty or error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<com.example.sayamusicc.network.models.TrackResponse> call, Throwable t) {
                Log.e("MainActivity", "API failure", t);
            }
        });
    }

    private void playUrl(String url, String title){
        if (url == null || url.isEmpty()) {
            tvNowPlaying.setText("No audio URL");
            return;
        }
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        tvNowPlaying.setText(title);
        btnPause.setText("Pause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

    // Adapter (inner class)
    private class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.TrackViewHolder> {
        private List<Track> tracks;

        TracksAdapter(List<Track> tracks){
            this.tracks = tracks;
        }

        void setTracks(List<Track> newTracks){
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
            holder.tvArtist.setText(t.getArtistName() != null ? t.getArtistName() : "");
            Glide.with(MainActivity.this)
                    .load(t.getAlbumImage())
                    .placeholder(android.R.drawable.ic_media_play)
                    .into(holder.imgCover);

            holder.btnPlay.setOnClickListener(v -> {
                String audio = t.getAudio();
                if (audio == null || audio.isEmpty()) {
                    // fallback: use tracks/file endpoint which redirects to actual file
                    // build URL: https://api.jamendo.com/v3.0/tracks/file/?client_id=...&id=TRACK_ID&action=stream
                    String fallback = "https://api.jamendo.com/v3.0/tracks/file/?client_id="
                            + BuildConfig.JAMENDO_CLIENT_ID
                            + "&id=" + t.getId()
                            + "&action=stream";
                    playUrl(fallback, t.getName() + " — " + t.getArtistName());
                } else {
                    playUrl(audio, t.getName() + " — " + t.getArtistName());
                }
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
            TrackViewHolder(View itemView){
                super(itemView);
                imgCover = itemView.findViewById(R.id.imgCover);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvArtist = itemView.findViewById(R.id.tvArtist);
                btnPlay = itemView.findViewById(R.id.btnPlay);
            }
        }
    }
}
