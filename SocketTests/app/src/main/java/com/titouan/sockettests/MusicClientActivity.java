package com.titouan.sockettests;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class MusicClientActivity extends ActionBarActivity {

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
        setContentView(R.layout.activity_music_client);

        lvSongs = (ListView) findViewById(R.id.songs);
        btnPlayPause = (ImageButton) findViewById(R.id.play_pause);
        tvSong = (TextView) findViewById(R.id.song_title);
        tvArtist = (TextView) findViewById(R.id.song_artist);

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeDiscoveryListener();
        mNsdHelper.discoverServices();

        updateUIHandler = new Handler();
    }

    public void onClick(View view){

        switch(view.getId()){
            case R.id.play_pause:
                if(socket!=null) {
                    out.println(isPlaying ? Command.PAUSE : Command.PLAY);
                    out.flush();
                    //isPlaying = !isPlaying;
                    //btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_play_arrow_white_24dp : R.drawable.ic_pause_white_24dp);
                }
                break;
        }

    }

    public void songPicked(View view){
        Song pickedSong = songsList.get((Integer) view.getTag());
        out.println(pickedSong.getId());
        out.flush();
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

                //get the list of songs sent by server
                songsList = new ArrayList<>();
                JSONArray musicsArray = new JSONArray(in.readLine());
                for(int i=0; i<musicsArray.length(); i++){
                    Song s = new Song(musicsArray.getJSONObject(i));
                    songsList.add(s);
                }

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
                                    Toast.makeText(MusicClientActivity.this, "Connexion with server lost.", Toast.LENGTH_SHORT).show();
                                }
                            });
                            startActivity(new Intent(MusicClientActivity.this, MainActivity.class));
                            MusicClientActivity.this.finish();
                            return;
                        }else{
                            Log.e("Received", read);
                            if(read.equals(State.PAUSED)){
                                isPlaying = false;
                                updateUIHandler.post(new UpdateBtnPlayPause());
                            }else if(read.equals(State.PLAYING)){
                                isPlaying = true;
                                updateUIHandler.post(new UpdateBtnPlayPause());
                            }else {
                                Song actualSong = new Song(read);
                                updateUIHandler.post(new UpdateSongView(actualSong));
                            }
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
            SongAdapter songAdapter = new SongAdapter(MusicClientActivity.this, songsList);
            lvSongs.setAdapter(songAdapter);
        }
    }

    class UpdateBtnPlayPause implements Runnable {

        @Override
        public void run() {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp);
            btnPlayPause.setEnabled(true);
        }
    }

    class UpdateSongView implements Runnable {
        Song song;

        public UpdateSongView(Song song){
            this.song = song;
        }

        @Override
        public void run(){
            tvSong.setText(song.getTitle());
            tvArtist.setText(song.getArtist());
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(socket != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    out.println(Command.VOLUME_DOWN);
                    out.flush();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_UP:
                    out.println(Command.VOLUME_UP);
                    out.flush();
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
        }else{
            return super.onKeyDown(keyCode, event);
        }
    }
}
