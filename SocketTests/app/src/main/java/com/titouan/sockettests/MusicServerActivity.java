package com.titouan.sockettests;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;
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
import java.util.List;


public class MusicServerActivity extends ActionBarActivity {

    private ServerSocket serverSocket;
    private Handler updateConversationHandler;
    private Thread serverThread = null;
    private List<CommunicationThread> clientThreads;
    private int localPort = -1;

    private TextView text;
    private EditText message;

    private NsdHelper mNsdHelper;

    //MediaPlayer attributes
    MediaPlayer mediaPlayer;
    JSONArray musicsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        text = (TextView) findViewById(R.id.text);
        message = (EditText) findViewById(R.id.message);


        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        text.setText(ip + "\n");

        clientThreads = new ArrayList<>();

        updateConversationHandler = new Handler();

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaPlayer = MediaPlayer.create(this, R.raw.music);

    }


    class ServerThread implements Runnable {

        public void run() {

            musicsList = getSongsList();

            Socket socket = null;
            try {
                serverSocket = new ServerSocket(0);
                localPort = serverSocket.getLocalPort();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mNsdHelper = new NsdHelper(MusicServerActivity.this);
            mNsdHelper.initializeRegistrationListener();
            mNsdHelper.registerService(localPort);

            updateConversationHandler.post(new UpdateUIThread("Port : " + localPort));

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket);
                    clientThreads.add(commThread);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
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

            send(musicsList.toString());

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = in.readLine();
                    if (read == null) {
                        updateConversationHandler.post(new UpdateUIThread("Connexion closed."));
                        close();
                        return;
                    }
                    if (read.equals("Play")) {
                        mediaPlayer.start();
                    } else if (read.equals("Pause")) {
                        mediaPlayer.pause();
                    } else {
                        try {
                            long newSongId = Integer.parseInt(read);
                            mediaPlayer.reset();
                            Uri newSongUri = ContentUris.withAppendedId(
                                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    newSongId);
                            mediaPlayer.setDataSource(MusicServerActivity.this, newSongUri);
                            mediaPlayer.prepare();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    updateConversationHandler.post(new UpdateUIThread(read));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void send(String message) {
            out.println(message);
            out.flush();
        }

        public void close() {
            try {
                in.close();
                out.close();
                clientSocket.close();
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

    public void send(View v) {
        for (CommunicationThread thread : clientThreads) {
            thread.send(message.getText().toString());
        }
        message.setText("");
        //TODO
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
            serverThread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to get the list of songs stored on the phone memory
     * @return JSONArray containing the list of songs
     */
    public JSONArray getSongsList() {

        JSONArray musicsArray = new JSONArray();

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
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);

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
