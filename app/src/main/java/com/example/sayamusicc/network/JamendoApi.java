package com.example.sayamusicc.network;

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


}
