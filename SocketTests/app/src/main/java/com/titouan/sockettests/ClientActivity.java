package com.titouan.sockettests;

import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;


public class ClientActivity extends ActionBarActivity {

    private Socket socket;

    private EditText message;
    private TextView tvMessage;
    private Handler updateConversationHandler;

    private PrintWriter out = null;
    private BufferedReader in = null;

    private NsdHelper mNsdHelper;

    private Thread clientThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("State", "Create");
        setContentView(R.layout.activity_client);

        message = (EditText) findViewById(R.id.message);
        tvMessage = (TextView) findViewById(R.id.text);

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeDiscoveryListener();
        mNsdHelper.discoverServices();

        updateConversationHandler = new Handler();
    }

    public void send(View v){
        out.println(message.getText().toString());
        out.flush();
        message.setText("");
    }

    public void connectToServer(InetAddress hostAddress, int hostPort){
        clientThread = new Thread(new ClientThread(hostAddress, hostPort));
        clientThread.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_client, menu);
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

                updateConversationHandler.post(new UpdateUIThread("Connected."));

                out = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while(!Thread.currentThread().isInterrupted()){
                    try{
                        String read = in.readLine();
                        if(read == null){
                            updateConversationHandler.post(new UpdateUIThread("Connexion closed."));
                            out.close();
                            in.close();
                            socket.close();
                            return;
                        }

                        updateConversationHandler.post(new UpdateUIThread(read));
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

    class UpdateUIThread implements Runnable {
        private String msg;

        public UpdateUIThread(String str){
            this.msg = str;
        }

        @Override
        public void run(){
            display(this.msg);
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
    protected void onResume() {
        Log.e("State", "Resume");
        super.onResume();
    }

    private void display(String str){
        tvMessage.setText(str+"\n"+tvMessage.getText().toString());
    }

    @Override
    protected void onDestroy() {
        Log.e("State", "Destroy");
        super.onDestroy();
        try {
            clientThread.interrupt();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
