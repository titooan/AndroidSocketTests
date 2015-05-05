package com.titouan.sockettests;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
/**
 * Created by titouan on 18/04/15.
 */
public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;
    private LayoutInflater songInflater;

    public SongAdapter(Context context ,ArrayList<Song> songs) {
        this.songs = songs;
        this.songInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int i) {
        return songs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        //map to song layout
        LinearLayout songLay = (LinearLayout)songInflater.inflate(R.layout.song, parent, false);

        TextView tvTitle  = (TextView) songLay.findViewById(R.id.song_title);
        TextView tvArtist = (TextView) songLay.findViewById(R.id.song_artist);
        TextView duration = (TextView) songLay.findViewById(R.id.song_duration);
        ImageView songImg = (ImageView) songLay.findViewById(R.id.song_img);

        Song currentSong = songs.get(position);

        tvTitle.setText(currentSong.getTitle());
        tvArtist.setText(currentSong.getArtist());
        duration.setText(ConvertSecondToHHMMSSString(currentSong.getDuration()));

        //if(currentSong.getCover()!=null){
        //    songImg.setImageBitmap(currentSong.getCover());
        //}else{
            songImg.setImageResource(R.mipmap.no_image_available);
        //}

        songLay.setTag(position);
        return songLay;
    }


    private String ConvertSecondToHHMMSSString(long mnSecondTime) {

        long nSecondTime = mnSecondTime /1000;

        long hours = nSecondTime / 3600;
        long minutes = (nSecondTime % 3600) / 60;
        long seconds = nSecondTime % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
