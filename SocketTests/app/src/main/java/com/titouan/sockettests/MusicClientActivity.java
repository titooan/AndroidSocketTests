package com.titouan.sockettests;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
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
    private Button btnPlay;
    private Button btnPause;

    private Handler updateUIHandler;

    private PrintWriter out = null;
    private BufferedReader in = null;

    private NsdHelper mNsdHelper;

    private Thread clientThread;

    private ArrayList<Song> songsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_client);

        lvSongs = (ListView) findViewById(R.id.songs);
        btnPause = (Button) findViewById(R.id.pause);
        btnPlay = (Button) findViewById(R.id.play);

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeDiscoveryListener();
        mNsdHelper.discoverServices();

        updateUIHandler = new Handler();
    }

    public void onClick(View view){

        switch(view.getId()){
            case R.id.play:
                out.println("Play");
                out.flush();
                break;
            case R.id.pause:
                out.println("Pause");
                out.flush();
                break;
        }

    }

    public void songPicked(View view){
        out.println(songsList.get((Integer) view.getTag()).getId());
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
                        }
                    }catch(IOException e){
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
            btnPause.setEnabled(true);
            btnPlay.setEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        Log.e("State", "Pause");
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.e("State", "Destroy");
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
