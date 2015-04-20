package com.titouan.sockettests;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MusicServerActivity extends ActionBarActivity {

    private ServerSocket serverSocket;
    private Handler updateUIHandler;
    private Thread serverThread = null;
    private List<CommunicationThread> clientThreads;
    private int localPort = -1;

    private TextView text;
    private TextView tvSong;
    private TextView tvArtist;

    private NsdHelper mNsdHelper;

    //MediaPlayer attributes
    private MediaPlayer mediaPlayer;
    private JSONArray musicsList;
    private Map<Long, Song> songsList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_server);

        text = (TextView) findViewById(R.id.text);
        tvSong = (TextView) findViewById(R.id.song_title);
        tvArtist = (TextView) findViewById(R.id.song_artist);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        text.setText(ip + "\n");

        clientThreads = new ArrayList<>();

        updateUIHandler = new Handler();

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaPlayer = MediaPlayer.create(this, R.raw.music);

    }


    class ServerThread implements Runnable {

        public void run() {

            musicsList = getSongsList();

            try {
                serverSocket = new ServerSocket(0);
                localPort = serverSocket.getLocalPort();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mNsdHelper = new NsdHelper(MusicServerActivity.this);
            mNsdHelper.initializeRegistrationListener();
            mNsdHelper.registerService(localPort);

            //Register service on network, publish IP and Port
            updateUIHandler.post(new UpdateUIThread("Port : " + localPort));

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket);
                    clientThreads.add(commThread);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    //Terminate thread when socket is closed.
                    return;
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private PrintWriter out = null;
        private BufferedReader in = null;
        private Socket clientSocket;

        CommunicationThread(Socket clientSocket) {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()))
                        , true);

                this.clientSocket = clientSocket;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            //When a connexion is opened by a client, we start by sending him the list of songs
            send(musicsList.toString());

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = in.readLine();
                    if (read == null) {
                        updateUIHandler.post(new UpdateUIThread("Connexion closed."));
                        close();
                        return;
                    }

                    //Check if the string received is a command or a song ID
                    if (read.equals(Command.PLAY)) {
                        mediaPlayer.start();
                        sendToAll(State.PLAYING);
                    } else if (read.equals(Command.PAUSE)) {
                        mediaPlayer.pause();
                        sendToAll(State.PAUSED);
                    }else if(read.equals(Command.VOLUME_DOWN)) {
                        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    }else if(read.equals(Command.VOLUME_UP)) {
                        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    }else {
                        try {
                            long newSongId = Long.parseLong(read);
                            mediaPlayer.reset();
                            Uri newSongUri = ContentUris.withAppendedId(
                                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    newSongId);
                            mediaPlayer.setDataSource(MusicServerActivity.this, newSongUri);
                            mediaPlayer.prepare();
                            sendToAll(songsList.get(newSongId).getJSONObject().toString());
                            sendToAll(State.PAUSED);
                            updateUIHandler.post(new UpdateSongView(songsList.get(newSongId)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    updateUIHandler.post(new UpdateUIThread(read));

                } catch (IOException e) {
                    updateUIHandler.post(new UpdateUIThread("Connexion closed."));
                    close();
                    return;
                }
            }
        }

        public void send(String message) {
            out.println(message);
            out.flush();
        }

        public void close() {
            try {
                clientSocket.close();
                in.close();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    class UpdateUIThread implements Runnable {
        private String msg;

        public UpdateUIThread(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            text.setText(text.getText().toString() + msg + "\n");
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

    /**
     * Methode using to send a message to all clients
     * @param message
     */
    public void sendToAll(String message) {
        for (CommunicationThread thread : clientThreads) {
            thread.send(message);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNsdHelper.tearDown();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        try {
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(CommunicationThread thread : clientThreads){
            thread.close();
        }
    }

    /**
     * Method used to get the list of songs stored on the phone memory
     * @return JSONArray containing the list of songs
     */
    public JSONArray getSongsList() {

        JSONArray musicsArray = new JSONArray();
        songsList = new HashMap<>();

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
            //add songs to list
            do {
                JSONObject music = new JSONObject();
                long thisId = musicCursor.getInt(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);

                songsList.put(thisId, new Song(thisId, thisTitle, thisArtist));

                try {
                    music.put("id", thisId);
                    music.put("title", thisTitle);
                    music.put("artist", thisArtist);
                    musicsArray.put(music);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            while (musicCursor.moveToNext());
        }

        return musicsArray;
    }


}
