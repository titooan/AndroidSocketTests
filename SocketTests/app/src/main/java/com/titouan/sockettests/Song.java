package com.titouan.sockettests;

import org.json.JSONObject;

/**
 * Created by titouan on 17/04/15.
 */
public class Song {

    private long id;
    private String title;
    private String artist;

    public Song(long id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
    }

    public Song(JSONObject json){
        try {
            this.id = json.getLong("id");
            this.title = json.getString("title");
            this.artist = json.getString("artist");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    @Override
    public String toString() {
        return "Song{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                '}';
    }
}
