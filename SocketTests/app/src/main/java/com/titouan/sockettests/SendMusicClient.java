package com.titouan.sockettests;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class SendMusicClient extends ActionBarActivity {

    private Socket socket;

    private ListView lvSongs;
    private ImageButton btnPlayPause;
    private TextView tvSong;
    private TextView tvArtist;

    private Handler updateUIHandler;

    private PrintWriter out = null;
    private BufferedReader in = null;

    private NsdHelper mNsdHelper;

    private Thread clientThread;

    private ArrayList<Song> songsList;

    /**
     * Boolean used to know is server is playing or not
     */
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_music_client);

        lvSongs = (ListView) findViewById(R.id.songs);
        btnPlayPause = (ImageButton) findViewById(R.id.play_pause);
        tvSong = (TextView) findViewById(R.id.song_title);
        tvArtist = (TextView) findViewById(R.id.song_artist);

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeDiscoveryListener();
        mNsdHelper.discoverServices();

        songsList = getSongsList();

        updateUIHandler = new Handler();



        new Thread(new Runnable() {
            @Override
            public void run() {
                songsList = getSongsList();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SongAdapter songAdapter = new SongAdapter(SendMusicClient.this, songsList);
                        lvSongs.setAdapter(songAdapter);
                    }
                });
            }
        }).start();
    }

    public void onClick(View view){

        switch(view.getId()){
            case R.id.play_pause:
                if(socket!=null) {

                    //isPlaying = !isPlaying;
                    //btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_play_arrow_white_24dp : R.drawable.ic_pause_white_24dp);
                }
                break;
        }

    }

    public void songPicked(View view){
        Song pickedSong = songsList.get((Integer) view.getTag());

        try {

            File file = new File (pickedSong.getPath());
            byte[] bytes = FileUtils.readFileToByteArray(file);

            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            os.flush();
            System.out.println(pickedSong);
            os.writeObject("send");
            if (pickedSong.getTitle()!=null) {
                os.writeObject(pickedSong.getTitle());
            }else{
                os.writeObject("new song");
            }
            os.writeObject(bytes);
            System.out.println("music sent");

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

            int path = musicCursor.getColumnIndex(MediaStore.Images.Media.DATA);

            //add songs to list
            do {

                list.add(new Song(
                        musicCursor.getLong(idColumn),
                        musicCursor.getString(titleColumn),
                        musicCursor.getString(artistColumn),
                        musicCursor.getLong(duration),
                        musicCursor.getString(path)

                ));

            }
            while (musicCursor.moveToNext());
        }
        return list;
    }


    public void connectToServer(InetAddress hostAddress, int hostPort){
        clientThread = new Thread(new ClientThread(hostAddress, hostPort));
        clientThread.start();
    }


    class ClientThread implements Runnable {

        private InetAddress hostAddress;
        private int hostPort;

        ClientThread(InetAddress hostAddress, int hostPort) {
            this.hostAddress = hostAddress;
            this.hostPort = hostPort;
        }



        @Override
        public void run(){
            try{
                socket = new Socket(hostAddress, hostPort);

                mNsdHelper.stopDiscovery();

                out = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));



                updateUIHandler.post(new UpdateListView());

                while(!Thread.currentThread().isInterrupted()){
                    try{
                        String read = in.readLine();
                        if(read == null){
                            out.close();
                            in.close();
                            socket.close();
                            mNsdHelper.discoverServices();
                            Thread.currentThread().interrupt();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SendMusicClient.this, "Connexion with server lost.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            startActivity(new Intent(SendMusicClient.this, MainActivity.class));
                            SendMusicClient.this.finish();
                            return;
                        }else{
                            Log.e("Received", read);
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }

            }catch(UnknownHostException e1){
                e1.printStackTrace();
            }catch(IOException e2){
                e2.printStackTrace();
            }catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    class UpdateListView implements Runnable {
        @Override
        public void run(){
            SongAdapter songAdapter = new SongAdapter(SendMusicClient.this, songsList);
            lvSongs.setAdapter(songAdapter);
        }
    }



    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if(clientThread != null) {
                clientThread.interrupt();
            }
            if(socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}