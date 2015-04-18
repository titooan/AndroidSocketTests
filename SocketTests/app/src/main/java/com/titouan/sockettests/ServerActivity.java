package com.titouan.sockettests;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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


public class ServerActivity extends ActionBarActivity {

    private ServerSocket serverSocket;
    private Handler updateConversationHandler;
    private Thread serverThread = null;
    private List<CommunicationThread> clientThreads;
    private int localPort = -1;

    private TextView text;
    private EditText message;

    private NsdHelper mNsdHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        text = (TextView) findViewById(R.id.text);
        message = (EditText) findViewById(R.id.message);

        //Display IP address
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        text.setText(ip + "\n");

        clientThreads = new ArrayList<>();

        updateConversationHandler = new Handler();

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

    }

    class ServerThread implements Runnable {
        public void run() {

            try {
                serverSocket = new ServerSocket(0);
                localPort = serverSocket.getLocalPort();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Register service on network, publish IP and Port
            mNsdHelper = new NsdHelper(ServerActivity.this);
            mNsdHelper.initializeRegistrationListener();
            mNsdHelper.registerService(localPort);

            updateConversationHandler.post(new UpdateUIThread("Port : " + localPort));

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();

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
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = in.readLine();

                    if (read == null) {
                        //Connexion interrupted from client
                        updateConversationHandler.post(new UpdateUIThread("Connexion closed."));
                        close();
                        return;
                    }
                    updateConversationHandler.post(new UpdateUIThread(read));

                } catch (IOException e) {
                    updateConversationHandler.post(new UpdateUIThread("Connexion closed."));
                    close();
                    return;
                }
            }
            close();
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
            text.setText(msg + "\n" + text.getText().toString());
        }
    }

    public void send(View v) {
        //Send message to all clients
        for (CommunicationThread thread : clientThreads) {
            thread.send(message.getText().toString());
        }
        message.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNsdHelper.tearDown();
        try {
            serverThread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (CommunicationThread thread : clientThreads) {
            thread.close();
        }
    }
}
