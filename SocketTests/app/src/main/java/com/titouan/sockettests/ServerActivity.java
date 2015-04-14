package com.titouan.sockettests;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
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
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;


public class ServerActivity extends ActionBarActivity {

    private ServerSocket serverSocket;
    private Handler updateConversationHandler;
    private Thread serverThread = null;
    private List<CommunicationThread> clientThreads;
    private int localPort = -1;

    private TextView text;
    private EditText message;

    private NsdHelper mNsdHelper;

    //Encryption constants
    private static final String RSA_ALGORITHM = "RSA";
    private static final String AES_ALGORITHM = "AES/CBC/PKCS7Padding";
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_FILE = "private.key";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        text = (TextView) findViewById(R.id.text);
        message = (EditText) findViewById(R.id.message);


        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        text.setText(ip+"\n");

        clientThreads = new ArrayList<>();

        updateConversationHandler = new Handler();

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

        /*
        // Encryption tests
        try {
            //Getting RSA PublicKey from file
            ObjectInputStream inputStream = new ObjectInputStream(getAssets().open(PUBLIC_KEY_FILE));
            final PublicKey publicKey = (PublicKey) inputStream.readObject();
            inputStream = new ObjectInputStream(getAssets().open(PRIVATE_KEY_FILE));
            final PrivateKey privateKey = (PrivateKey) inputStream.readObject();

            final String originalText = "Text to be encrypted ";
            final byte[] cipherText = encrypt(originalText, publicKey);
            final String plainText = decrypt(cipherText, privateKey);

            String encrypted = new String(rsaEncrypt("Bonjour".getBytes(), publicKey));
            display(new String(cipherText));
            display(plainText);
        }catch(Exception e){
            display("Encryption failed.");
            e.printStackTrace();
        }*/
    }

    private void display(String str){
        text.setText(str+"\n"+text.getText().toString());
    }

    class ServerThread implements Runnable{
        public void run(){
            Socket socket = null;
            try{
                serverSocket = new ServerSocket(0);
                localPort = serverSocket.getLocalPort();
            }catch (IOException e){
                e.printStackTrace();
            }

            mNsdHelper = new NsdHelper(ServerActivity.this);
            mNsdHelper.initializeRegistrationListener();
            mNsdHelper.registerService(localPort);

            updateConversationHandler.post(new UpdateUIThread("Port : "+localPort));

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNsdHelper.tearDown();
        try{
            serverSocket.close();
            serverThread.interrupt();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Encrypt the text using public key
     * @param text original text/key/iv
     * @return
     */
    private byte[] rsaEncrypt(byte[] text, PublicKey key) {
        byte[] cipherText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipherText;
    }

    /**
     * Encrypt the plain text using public key.
     *
     * @param text
     *          : original plain text
     * @param key
     *          :The public key
     * @return Encrypted text
     * @throws java.lang.Exception
     */
    public static byte[] encrypt(String text, PublicKey key) {
        byte[] cipherText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(text.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cipherText;
    }

    /**
     * Decrypt text using private key.
     *
     * @param text
     *          :encrypted text
     * @param key
     *          :The private key
     * @return plain text
     * @throws java.lang.Exception
     */
    public static String decrypt(byte[] text, PrivateKey key) {
        byte[] dectyptedText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);

            // decrypt the text using the private key
            cipher.init(Cipher.DECRYPT_MODE, key);
            dectyptedText = cipher.doFinal(text);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new String(dectyptedText);
    }


}
