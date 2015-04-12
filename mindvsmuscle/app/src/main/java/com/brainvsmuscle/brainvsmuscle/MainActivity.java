package com.brainvsmuscle.brainvsmuscle;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import bluetooth_utilities.Bluetooth_Blend_Interface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.interaxon.libmuse.Accelerometer;
import com.interaxon.libmuse.ConnectionState;
import com.interaxon.libmuse.Eeg;
import com.interaxon.libmuse.LibMuseVersion;
import com.interaxon.libmuse.Muse;
import com.interaxon.libmuse.MuseArtifactPacket;
import com.interaxon.libmuse.MuseConnectionListener;
import com.interaxon.libmuse.MuseConnectionPacket;
import com.interaxon.libmuse.MuseDataListener;
import com.interaxon.libmuse.MuseDataPacket;
import com.interaxon.libmuse.MuseDataPacketType;
import com.interaxon.libmuse.MuseManager;
import com.interaxon.libmuse.MusePreset;
import com.interaxon.libmuse.MuseVersion;

public class MainActivity extends ActionBarActivity {

    Bluetooth_Blend_Interface BTAI; // BlueTooth Arduino Interface (BTAI) object
    boolean enable_BT_arm = true;
    boolean isBTConnected;
    Button mConnectBtn;

    public void setBTConnected(boolean toSet){
        isBTConnected = toSet;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(enable_BT_arm){
            BTAI = new Bluetooth_Blend_Interface(this, enable_BT_arm);
            registerReceiver(BTAI.get_mGattUpdateReceiver(), Bluetooth_Blend_Interface.makeGattUpdateIntentFilter());
        }
        mConnectBtn = (Button) findViewById(R.id.connect_btn);
        mConnectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!isBTConnected && enable_BT_arm){
                    BTAI.connect();
                }else{
                    BTAI.disconnect();
                }
            }
        });
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

    @Override
    public void onResume() {
        super.onResume();
        if (BTAI != null) {
            registerReceiver(BTAI.get_mGattUpdateReceiver(), Bluetooth_Blend_Interface.makeGattUpdateIntentFilter());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(BTAI != null)
            unregisterReceiver(BTAI.get_mGattUpdateReceiver());
    }



    byte[] result = new byte[1] {
        //0xFF << get this value
    };

}
