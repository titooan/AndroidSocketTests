package com.titouan.sockettests;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;


public class MusicActivity extends ActionBarActivity {

    private MediaPlayer mediaPlayer;

    private ArrayList<Song> songsList;
    private ListView lvSongs;
    private Button btnPlay;
    private Button btnPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        lvSongs = (ListView) findViewById(R.id.songs);
        btnPause = (Button) findViewById(R.id.pause);
        btnPlay = (Button) findViewById(R.id.play);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mediaPlayer = new MediaPlayer();

        new Thread(new Runnable() {
            @Override
            public void run() {
                songsList = getSongsList();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SongAdapter songAdapter = new SongAdapter(MusicActivity.this, songsList);
                        lvSongs.setAdapter(songAdapter);
                    }
                });
            }
        }).start();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play:
                mediaPlayer.start();
                break;
            case R.id.pause:
                mediaPlayer.pause();
                break;
        }
    }

    public void songPicked(View view) {

        try {
            mediaPlayer.reset();
            Uri newSongUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    songsList.get((Integer) view.getTag()).getId());
            mediaPlayer.setDataSource(MusicActivity.this, newSongUri);
            mediaPlayer.prepare();
            btnPlay.setEnabled(true);
            btnPause.setEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * Method used to get the list of songs stored on phone
     *
     * @return songsList
     */
    public ArrayList<Song> getSongsList() {
        ArrayList<Song> list = new ArrayList<>();
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int duration = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);

            Bitmap cover = getAlbumart(ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    musicCursor.getLong(idColumn)));

            //add songs to list
            do {

                list.add(new Song(
                        musicCursor.getLong(idColumn),
                        musicCursor.getString(titleColumn),
                        musicCursor.getString(artistColumn),
                        musicCursor.getLong(duration)

                ));

            }
            while (musicCursor.moveToNext());
        }
        return list;
    }

    public Bitmap getAlbumart(Uri uri) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this.getApplicationContext(),uri);
        Bitmap bitmap=null;

        byte [] data = mmr.getEmbeddedPicture();
        //coverart is an Imageview object

        // convert the byte array to a bitmap
        if(data != null)
        {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        }

        return bitmap;
    }
}
