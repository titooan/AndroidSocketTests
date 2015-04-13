package com.titouan.sockettests;

import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
import java.net.Socket;
import java.net.UnknownHostException;


public class ClientActivity extends ActionBarActivity {

    private Socket socket;

    private EditText message;
    private TextView tvMessage;
    private Handler updateConversationHandler;

    private static final String SERVER_IP = "192.168.1.22";
    public static final int SERVER_PORT = ServerActivity.SERVERPORT;

    private PrintWriter out = null;
    private BufferedReader in = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        message = (EditText) findViewById(R.id.message);
        tvMessage = (TextView) findViewById(R.id.text);

        new Thread(new ClientThread()).start();

        updateConversationHandler = new Handler();
    }

    public void send(View v){
        out.println(message.getText().toString());
        out.flush();
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

        @Override
        public void run(){
            tvMessage.setText("Try");
            try{
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVER_PORT);
                updateConversationHandler.post(new UpdateUIThread("Connected."));
            }catch(UnknownHostException e1){
                e1.printStackTrace();
            }catch(IOException e2){
                e2.printStackTrace();
            }

            try {
                out = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (Exception e) {
                e.printStackTrace();
            }

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
        }
    }

    class UpdateUIThread implements Runnable {
        private String msg;

        public UpdateUIThread(String str){
            this.msg = str;
        }

        @Override
        public void run(){
            tvMessage.setText(tvMessage.getText().toString()+msg+"\n");
        }
    }
}
