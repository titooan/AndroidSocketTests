package com.titouan.sockettests;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void onClick(View view){
        switch(view.getId()){
            case R.id.server:
                startActivity(new Intent(this, ServerActivity.class));
                break;
            case R.id.client:
                startActivity(new Intent(this, ClientActivity.class));
                break;
            case R.id.music:
                startActivity(new Intent(this, MusicActivity.class));
                break;
            case R.id.musicServer:
                startActivity(new Intent(this, MusicServerActivity.class));
                break;
            case R.id.musicClient:
                startActivity(new Intent(this, MusicClientActivity.class));
                break;
            case R.id.musicSendClient:
                startActivity(new Intent(this, SendMusicClient.class));
                break;
            case R.id.musicSendServer:
                startActivity(new Intent(this, SendMusicServer.class));
                break;
        }
    }
}
