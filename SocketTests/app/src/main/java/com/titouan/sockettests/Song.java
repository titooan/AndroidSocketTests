package com.titouan.sockettests;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by titouan on 17/04/15.
 */
public class Song {

    private long id;
    private String title;
    private String artist;
    private long duration;
    private String path;
    private String musicContent;

    public Song(long id, String title, String artist, Long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
    }

    public Song(long id, String title, String artist, Long duration, String path) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.path=path;
    }

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
            this.duration = json.getLong("duration");
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public Song(String json) throws JSONException{
        this(new JSONObject(json));
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

    public JSONObject getJSONObject(){
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("title", title);
            json.put("artist", artist);
            json.put("duration", duration);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public JSONObject getJSONObjectWithContent(){
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("title", title);
            json.put("artist", artist);
            json.put("duration", duration);
            json.put("content", musicContent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public void setMusicContent(String musicContent) {
        this.musicContent = musicContent;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDuration() {
        return duration;
    }

    public String getMusicContent() {
        return musicContent;
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
