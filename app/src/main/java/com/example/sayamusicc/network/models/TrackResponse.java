package com.example.sayamusicc.network.models;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class TrackResponse {
    @SerializedName("results")
    private List<Track> results;

    public List<Track> getResults() {
        return results;
    }
}
