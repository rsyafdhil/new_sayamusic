package com.example.sayamusicc.network;

import com.example.sayamusicc.network.models.Track;
import com.example.sayamusicc.network.models.TrackResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface JamendoApi {
    @GET("tracks/")
    Call<TrackResponse> getTracks(
            @Query("client_id") String clientId,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("order") String order
    );

    // Search Tracks
    @GET("tracks/")
    Call<TrackResponse> searchTracks(
            @Query("client_id") String clientId,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("search") String searchQuery,
            @Query("order") String order
    );

    // Search tracks by artist name
    @GET("tracks/")
    Call<TrackResponse> searchTracksByArtist(
            @Query("client_id") String clientId,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("artist_name") String artistName,
            @Query("order") String order
    );

    // Search tracks by track name
    @GET("tracks/")
    Call<TrackResponse> searchTracksByName(
            @Query("client_id") String clientId,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("name") String trackName,
            @Query("order") String order
    );
}
