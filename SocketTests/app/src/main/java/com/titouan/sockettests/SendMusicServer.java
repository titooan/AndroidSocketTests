package com.titouan.sockettests;

import android.app.ProgressDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class SendMusicServer extends ActionBarActivity {

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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_music_server);

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



            try {
                serverSocket = new ServerSocket(0);
                localPort = serverSocket.getLocalPort();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mNsdHelper = new NsdHelper(SendMusicServer.this);
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

        private ObjectOutputStream ous = null;
        private ObjectInputStream is = null;
        private Socket clientSocket;

        CommunicationThread(Socket clientSocket) {
            try {
                is = new ObjectInputStream(clientSocket.getInputStream());
                ous =  new ObjectOutputStream(clientSocket.getOutputStream());

                this.clientSocket = clientSocket;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            //When a connexion is opened by a client, we start by sending him the list of songs

            while (!Thread.currentThread().isInterrupted()) {
                try {

                    String read = new String((String)is.readObject());


                    if ("send".equals(read)) {

                        String title = new String((String)is.readObject());

                        updateUIHandler.post(new UpdateUIThread("Getting Music: " + title));
                        byte[] bytes = (byte[]) is.readObject();
                        if (bytes == null) {
                            updateUIHandler.post(new UpdateUIThread("Connexion closed."));
                            close();
                            return;
                        }


                        //new receiveMusicAsyncTask().execute(title.getBytes(),bytes);
                        saveMP3(title,bytes);
                        updateUIHandler.post(new UpdateUIThread("Music Received."));
                        System.out.println("music received");
                    }


                    //updateUIHandler.post(new UpdateUIThread(read));

                } catch (IOException e) {
                    updateUIHandler.post(new UpdateUIThread("Connexion closed."));
                    close();
                    return;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }


        public void close() {
            try {
                clientSocket.close();
                is.close();
                ous.close();
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        public void saveMP3(String title, byte[] bytes){
            File photo=new File(Environment.getExternalStorageDirectory(), title+".mp3");

            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos=new FileOutputStream(photo.getPath());
                fos.write(bytes);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e("MP3Demo", "Exception in mp3Callback", e);
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

    private class receiveMusicAsyncTask extends AsyncTask<byte[], Void, Void> {

        private ProgressDialog progress;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress = new ProgressDialog(SendMusicServer.this);
                    progress.setMessage("Receiving new song");
                    progress.show();
                }
            });
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progress.isShowing()) {
                        progress.dismiss();
                    }

                }
            });
        }

        @Override
        protected Void doInBackground(byte[]... bytes) {

            String title = new String(bytes[0]);

            File music=new File(Environment.getExternalStorageDirectory(), title+".mp3");

            if (music.exists()) {
                music.delete();
            }

            try {
                FileOutputStream fos=new FileOutputStream(music.getPath());
                fos.write(bytes[1]);
                fos.close();
            }
            catch (java.io.IOException e) {

            }
            return null;
        }
    }


}
