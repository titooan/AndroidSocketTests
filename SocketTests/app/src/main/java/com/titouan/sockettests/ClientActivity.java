package com.titouan.sockettests;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


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
        setContentView(R.layout.activity_client);

        message = (EditText) findViewById(R.id.message);
        tvMessage = (TextView) findViewById(R.id.text);

        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeDiscoveryListener();
        mNsdHelper.discoverServices();

        updateConversationHandler = new Handler();
    }

    public void send(View v) {
        out.println(message.getText().toString());
        out.flush();
        message.setText("");
    }

    public void connectToServer(InetAddress hostAddress, int hostPort) {
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
        public void run() {
            try {
                socket = new Socket(hostAddress, hostPort);

                mNsdHelper.stopDiscovery();

                updateConversationHandler.post(new UpdateUIThread("Connected."));

                out = new PrintWriter(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String read = in.readLine();
                        if (read == null) {
                            updateConversationHandler.post(new UpdateUIThread("Connexion closed."));
                            out.close();
                            in.close();
                            socket.close();
                            mNsdHelper.discoverServices();
                            return;
                        }

                        updateConversationHandler.post(new UpdateUIThread(read));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
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
            display(this.msg);
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
    protected void onResume() {
        super.onResume();
    }

    private void display(String str) {
        tvMessage.setText(str + "\n" + tvMessage.getText().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (clientThread != null) {
                clientThread.interrupt();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
