package com.example.sayamusicc.network.models;

import com.google.gson.annotations.SerializedName;

public class Track {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("artist_name")
    private String artistName;

    @SerializedName("album_name")
    private String albumName;

    @SerializedName("audio")
    private String audio; // may be null

    @SerializedName("album_image")
    private String albumImage;

    // getters
    public String getId(){ return id; }
    public String getName(){ return name; }
    public String getArtistName(){ return artistName; }
    public String getAlbumName(){ return albumName; }
    public String getAudio(){ return audio; }
    public String getAlbumImage(){ return albumImage; }
}
