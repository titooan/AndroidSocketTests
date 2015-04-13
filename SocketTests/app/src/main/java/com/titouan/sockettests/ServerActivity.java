package com.titouan.sockettests;

import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class ServerActivity extends ActionBarActivity {

    private ServerSocket serverSocket;
    private Handler updateConversationHandler;
    private Thread serverThread = null;
    private List<CommunicationThread> clientThreads;

    private TextView text;
    private EditText message;

    public static final int SERVERPORT = 6000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        text = (TextView) findViewById(R.id.text);
        message = (EditText) findViewById(R.id.message);


        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        text.setText(ip);

        clientThreads = new ArrayList<>();

        updateConversationHandler = new Handler();

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    class ServerThread implements Runnable{
        public void run(){
            Socket socket = null;
            try{
                serverSocket = new ServerSocket(SERVERPORT);
            }catch (IOException e){
                e.printStackTrace();
            }

            while(!Thread.currentThread().isInterrupted()){
                try{
                    socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket);
                    clientThreads.add(commThread);
                    new Thread(commThread).start();
                }catch(IOException e){
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
            try{
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(clientSocket.getOutputStream()))
                        ,true);

                this.clientSocket = clientSocket;

            }catch(IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try{
                    String read = in.readLine();
                    if(read == null){
                        updateConversationHandler.post(new UpdateUIThread("Connexion closed."));
                        close();
                        return;
                    }
                    updateConversationHandler.post(new UpdateUIThread(read));

                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }

        public void send(String message){
            out.println(message);
            out.flush();
        }

        public void close(){
            try {
                in.close();
                out.close();
                clientSocket.close();
            }catch (Exception e){
                e.printStackTrace();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    class UpdateUIThread implements Runnable {
        private String msg;

        public UpdateUIThread(String str){
            this.msg = str;
        }

        @Override
        public void run(){
            text.setText(text.getText().toString()+msg+"\n");
        }
    }

    public void send(View v){
        for(CommunicationThread thread : clientThreads){
            thread.send(message.getText().toString());
        }
        message.setText("");
        //TODO
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try{
            serverSocket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

}
