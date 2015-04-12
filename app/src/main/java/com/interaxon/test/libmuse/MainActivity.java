/**
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2015
 */

package com.interaxon.test.libmuse;

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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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

import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import bluetooth_utilities.Bluetooth_Blend_Interface;


/**
 * In this simple example MainActivity implements 2 MuseHeadband listeners
 * and updates UI when data from Muse is received. Similarly you can implement
 * listers for other data or register same listener to listen for different type
 * of data.
 * For simplicity we create Listeners as inner classes of MainActivity. We pass
 * reference to MainActivity as we want listeners to update UI thread in this
 * example app.
 * You can also connect multiple muses to the same phone and register same
 * listener to listen for data from different muses. In this case you will
 * have to provide synchronization for data members you are using inside
 * your listener.
 *
 * Usage instructions:
 * 1. Enable bluetooth on your device
 * 2. Pair your device with muse
 * 3. Run this project
 * 4. Press Refresh. It should display all paired Muses in Spinner
 * 5. Make sure Muse headband is waiting for connection and press connect.
 * It may take up to 10 sec in some cases.
 * 6. You should see EEG and accelerometer data as well as connection status,
 * Version information and MuseElements (alpha, beta, theta, delta, gamma waves)
 * on the screen.
 */
public class MainActivity extends Activity implements OnClickListener {

    final int SAMPLE_LENGTH = 20;
    /**
     * Connection listener updates UI with new connection status and logs it.
     */
    class ConnectionListener extends MuseConnectionListener {

        final WeakReference<Activity> activityRef;

        ConnectionListener(final WeakReference<Activity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(MuseConnectionPacket p) {
            final ConnectionState current = p.getCurrentConnectionState();
            final String status = p.getPreviousConnectionState().toString() +
                    " -> " + current;
            final String full = "Muse " + p.getSource().getMacAddress() +
                    " " + status;
            Log.i("Muse Headband", full);
            Activity activity = activityRef.get();
            // UI thread is used here only because we need to update
            // TextView values. You don't have to use another thread, unless
            // you want to run disconnect() or connect() from connection packet
            // handler. In this case creating another thread is required.
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView statusText =
                                (TextView) findViewById(R.id.con_status);
                        statusText.setText(status);
                        TextView museVersionText =
                                (TextView) findViewById(R.id.version);
                        if (current == ConnectionState.CONNECTED) {
                            MuseVersion museVersion = muse.getMuseVersion();
                            String version = museVersion.getFirmwareType() +
                                    " - " + museVersion.getFirmwareVersion() +
                                    " - " + Integer.toString(
                                    museVersion.getProtocolVersion());
                            museVersionText.setText(version);
                        } else {
                            museVersionText.setText(R.string.undefined);
                        }
                    }
                });
            }
        }
    }

    /**
     * Data listener will be registered to listen for: Accelerometer,
     * Eeg and Relative Alpha bandpower packets. In all cases we will
     * update UI with new values.
     * We also will log message if Artifact packets contains "blink" flag.
     * DataListener methods will be called from execution thread. If you are
     * implementing "serious" processing algorithms inside those listeners,
     * consider to create another thread.
     */
    class DataListener extends MuseDataListener {

        final WeakReference<Activity> activityRef;

        DataListener(final WeakReference<Activity> activityRef) {
            this.activityRef = activityRef;
        }

        double alpha = 0.0;
        double beta = 0.0;
        double gamma = 0.0;
        double theta = 0.0;
        int n = 175;
        ArrayList<Double> alphaList = new ArrayList<>();
        ArrayList<Double> betaList = new ArrayList<>();
        ArrayList<Double> gammaList = new ArrayList<>();
        ArrayList<Double> thetaList = new ArrayList<>();
        @Override
        public void receiveMuseDataPacket(MuseDataPacket p) {
            Log.i("", (p.getPacketType()).toString());
            if (p.getPacketType() == MuseDataPacketType.ALPHA_ABSOLUTE) {
                alpha = updateAlphaAbsolute(p.getValues());
                if ( alphaList.size() < n) {
                    alphaList.add(alpha);
                } else {
                    alphaList.remove(0);
                    alphaList.add(alpha);
                }
            }
            if (p.getPacketType() == MuseDataPacketType.BETA_ABSOLUTE) {
                beta = updateBetaAbsolute(p.getValues());
                if (betaList.size() < n) {
                    betaList.add(beta);
                } else {
                    betaList.remove(0);
                    betaList.add(beta);
                }
            }
            if (p.getPacketType() == MuseDataPacketType.GAMMA_ABSOLUTE) {
                gamma = updateGammaAbsolute(p.getValues());
                if (gammaList.size() < n) {
                    gammaList.add(gamma);
                } else {
                    gammaList.remove(0);
                    gammaList.add(gamma);
                }
            }
            if (p.getPacketType() == MuseDataPacketType.THETA_ABSOLUTE) {
                theta = updateThetaAbsolute(p.getValues());
                if (thetaList.size() < n) {
                    thetaList.add(theta);
                } else {
                    thetaList.remove(0);
                    thetaList.add(theta);
                }
            }

            double[] completeList= new double[alphaList.size() * 4];
            for (int i = 0; i < alphaList.size(); i++) {
                completeList[i] = alphaList.get(i);
                completeList[i + alphaList.size()] = betaList.get(i);
                completeList[i + alphaList.size() * 2] = gammaList.get(i);
                completeList[i + alphaList.size() * 3] = thetaList.get(i);
            }

            double score = network.compute(new BasicMLData(completeList)).getData(0);

            byte[] scores = new byte[1];
            scores[0] = (byte)(score * 255);
            Log.i("", score + "");
            BTAI.sendData(scores);
        }

        @Override
        public void receiveMuseArtifactPacket(MuseArtifactPacket p) {
            if (p.getHeadbandOn() && p.getBlink()) {
                Log.i("Artifacts", "blink");
            }
        }

        private void updateAccelerometer(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView acc_x = (TextView) findViewById(R.id.acc_x);
                        TextView acc_y = (TextView) findViewById(R.id.acc_y);
                        TextView acc_z = (TextView) findViewById(R.id.acc_z);
                        acc_x.setText(String.format(
                                "%6.2f", data.get(Accelerometer.FORWARD_BACKWARD.ordinal())));
                        acc_y.setText(String.format(
                                "%6.2f", data.get(Accelerometer.UP_DOWN.ordinal())));
                        acc_z.setText(String.format(
                                "%6.2f", data.get(Accelerometer.LEFT_RIGHT.ordinal())));
                    }
                });
            }
        }

        private int counter = 0;
        private double[][] averages = new double[4][200];
        private void updateEeg(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (counter >= 200) {
                            byte[] avg = new byte[1];
                            avg[0] = (byte) calculateAverage(averages,200);
                            BTAI.sendData(avg);
                            averages = new double[4][200];
                            counter = 0;
                        }

                        TextView tp9 = (TextView) findViewById(R.id.eeg_tp9);
                        TextView fp1 = (TextView) findViewById(R.id.eeg_fp1);
                        TextView fp2 = (TextView) findViewById(R.id.eeg_fp2);
                        TextView tp10 = (TextView) findViewById(R.id.eeg_tp10);
                        tp9.setText(String.format(
                                "%6.2f", data.get(Eeg.TP9.ordinal())));
                        fp1.setText(String.format(
                                "%6.2f", data.get(Eeg.FP1.ordinal())));
                        fp2.setText(String.format(
                                "%6.2f", data.get(Eeg.FP2.ordinal())));
                        tp10.setText(String.format(
                                "%6.2f", data.get(Eeg.TP10.ordinal())));

                        averages[0][counter] = data.get(Eeg.TP9.ordinal());
                        averages[1][counter] = data.get(Eeg.FP1.ordinal());
                        averages[2][counter] = data.get(Eeg.FP2.ordinal());
                        averages[3][counter] = data.get(Eeg.TP10.ordinal());
                        counter++;

                        // Update graph to reflect this set of samples
                        double[] allAverages = new double[SAMPLE_LENGTH];
                        for (int i = 0; i < SAMPLE_LENGTH; i++) {
                            allAverages[i] = averages[0][i] + averages[1][i] + averages[2][i] + averages[3][i];
                        }
                        updateChart((LineChart) findViewById(R.id.line_chart), allAverages);
                    }
                });
            }
        }

        /**
         * Updates the plot based on the newly sampled data points.
         *
         * @param chart: A LineChart object retrieved from the main UI via findViewById
         * @param dataPoints: A double array of length SAMPLE_LENGTH to plot
         */
        public void updateChart(LineChart chart, double[] dataPoints) {
            // Clear the chart first, if applicable
            try {
                chart.getData().removeDataSet(1);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Initialize x and y values for plotting
            ArrayList<String> xVals = new ArrayList<String>();
            for (int i = 0; i < SAMPLE_LENGTH; i++) {
                xVals.add((i) + "");
            }
            ArrayList<Entry> yVals = new ArrayList<Entry>();
            for (int i = 0; i < SAMPLE_LENGTH; i++) {
                yVals.add(new Entry((float)dataPoints[i], i));
            }

            // Create the data sets and add it to the plot's LineData
            LineDataSet dataSet = new LineDataSet(yVals, "DataSet 1");
            ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
            dataSets.add(dataSet);
            LineData data = new LineData(xVals, dataSets);

            // Hide axes and other text
            chart.setDescription("");
            chart.getAxisLeft().setDrawLabels(false);
            chart.getAxisRight().setDrawLabels(false);
            chart.getXAxis().setDrawLabels(false);
            chart.getLegend().setEnabled(false);

            // Set the data on the UI
            chart.setData(data);

            // Update the chart
            chart.notifyDataSetChanged();
            chart.invalidate();
        }

        private double calculateAverage(double[][] avgs, int total) {
            double sum = 0.0;
            for (int i = 0; i < avgs.length; i++)
                for (int j = 0; j < avgs[i].length; j++)
                    sum += avgs[i][j];
            return sum/total;
        }
        private double[][] averages_alpha = new double[2][50];
        private double final_alpha;
        private double updateAlphaAbsolute(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        TextView elem1 = (TextView) findViewById(R.id.elem1);
//                        TextView elem2 = (TextView) findViewById(R.id.elem2);
//                        TextView elem3 = (TextView) findViewById(R.id.elem3);
//                        TextView elem4 = (TextView) findViewById(R.id.elem4);
//                        elem1.setText(String.format(
//                                "%6.2f", data.get(Eeg.TP9.ordinal())));
//                        elem2.setText(String.format(
//                                "%6.2f", data.get(Eeg.FP1.ordinal())));
//                        elem3.setText(String.format(
//                                "%6.2f", data.get(Eeg.FP2.ordinal())));
//                        elem4.setText(String.format(
//                                "%6.2f", data.get(Eeg.TP10.ordinal())));
                        for (int i = 0; i < 50; i++) {
                            averages_alpha[0][i] = data.get(Eeg.FP1.ordinal());
                            averages_alpha[1][i] = data.get(Eeg.FP2.ordinal());
                        }
                        byte[] avg = new byte[1];
                        avg[0] = (byte) (calculateAverage(averages_alpha, 100) * 1000);
//                        String vals =  Byte.toString(avg[0]);
//                        Log_Utilities.Utilities.append_text_to_file("alpha.txt", vals + "\n");
                        averages_alpha = new double[2][50];
                        final_alpha = avg[0];
                    }
                });
            }
            return final_alpha;
        }
        private double[][] averages_beta = new double[4][50];
        private double final_beta;
        private double updateBetaAbsolute(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        TextView elem1 = (TextView) findViewById(R.id.elem1);
//                        TextView elem2 = (TextView) findViewById(R.id.elem2);
//                        TextView elem3 = (TextView) findViewById(R.id.elem3);
//                        TextView elem4 = (TextView) findViewById(R.id.elem4);
//                        elem1.setText(String.format(
//                                "%6.2f", data.get(Eeg.TP9.ordinal())));
//                        elem2.setText(String.format(
//                                "%6.2f", data.get(Eeg.FP1.ordinal())));
//                        elem3.setText(String.format(
//                                "%6.2f", data.get(Eeg.FP2.ordinal())));
//                        elem4.setText(String.format(
//                                "%6.2f", data.get(Eeg.TP10.ordinal())));
                        for (int i = 0; i < 50; i++) {
                            averages_beta[0][i] = data.get(Eeg.TP9.ordinal());
                            averages_beta[1][i] = data.get(Eeg.FP1.ordinal());
                            averages_beta[2][i] = data.get(Eeg.FP2.ordinal());
                            averages_beta[3][i] = data.get(Eeg.TP10.ordinal());
                        }
                        byte[] avg = new byte[1];
                        avg[0] = (byte) (calculateAverage(averages_beta, 200) * 1000);
//                        String vals =  Byte.toString(avg[0]);
//                        Log_Utilities.Utilities.append_text_to_file("beta.txt", vals + "\n");
                        averages_beta = new double[4][50];
                        final_beta = avg[0];
                    }
                });
            }
            return final_beta;
        }
        private double[][] averages_gamma = new double[4][50];
        private double final_gamma;
        private double updateGammaAbsolute(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        TextView elem1 = (TextView) findViewById(R.id.elem1);
//                        TextView elem2 = (TextView) findViewById(R.id.elem2);
//                        TextView elem3 = (TextView) findViewById(R.id.elem3);
//                        TextView elem4 = (TextView) findViewById(R.id.elem4);
//                        elem1.setText(String.format(
//                                "%6.2f", data.get(Eeg.TP9.ordinal())));
//                        elem2.setText(String.format(
//                                "%6.2f", data.get(Eeg.FP1.ordinal())));
//                        elem3.setText(String.format(
//                                "%6.2f", data.get(Eeg.FP2.ordinal())));
//                        elem4.setText(String.format(
//                                "%6.2f", data.get(Eeg.TP10.ordinal())));
                          for (int i = 0; i < 50; i++) {
                              averages_gamma[0][i] = data.get(Eeg.TP9.ordinal());
                              averages_gamma[1][i] = data.get(Eeg.FP1.ordinal());
                              averages_gamma[2][i] = data.get(Eeg.FP2.ordinal());
                              averages_gamma[3][i] = data.get(Eeg.TP10.ordinal());
                          }
                            byte[] avg = new byte[1];
                            avg[0] = (byte) (calculateAverage(averages_gamma, 200) * 1000);
//                            String vals =  Byte.toString(avg[0]);
//                            Log_Utilities.Utilities.append_text_to_file("gamma.txt", vals + "\n");
                            averages_gamma = new double[4][50];
                            final_gamma = avg[0];
                    }

                });
            }
            return final_gamma;
        }
        private double[][] averages_theta = new double[4][50];
        private double final_theta;
        private double updateThetaAbsolute(final ArrayList<Double> data) {
            Activity activity = activityRef.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        TextView elem1 = (TextView) findViewById(R.id.elem1);
//                        TextView elem2 = (TextView) findViewById(R.id.elem2);
//                        TextView elem3 = (TextView) findViewById(R.id.elem3);
//                        TextView elem4 = (TextView) findViewById(R.id.elem4);
//                        elem1.setText(String.format(
//                                "%6.2f", data.get(Eeg.TP9.ordinal())));
//                        elem2.setText(String.format(
//                                "%6.2f", data.get(Eeg.FP1.ordinal())));
//                        elem3.setText(String.format(
//                                "%6.2f", data.get(Eeg.FP2.ordinal())));
//                        elem4.setText(String.format(
//                                "%6.2f", data.get(Eeg.TP10.ordinal())));
                        for (int i = 0; i < 50; i++) {
                            averages_theta[0][i] = data.get(Eeg.TP9.ordinal());
                            averages_theta[1][i] = data.get(Eeg.FP1.ordinal());
                            averages_theta[2][i] = data.get(Eeg.FP2.ordinal());
                            averages_theta[3][i] = data.get(Eeg.TP10.ordinal());
                            counter++;
                            //return avg[0] somehow
                        }
                        byte[] avg = new byte[1];
                        avg[0] = (byte) (calculateAverage(averages_theta,200) * 1000);
//                        String vals =  Byte.toString(avg[0]);
//                        Log_Utilities.Utilities.append_text_to_file("theta.txt", vals + "\n");
                        averages_theta = new double[4][50];
                        final_theta = avg[0];
                    }
                });
            }
            return final_theta;
        }
    }

    private BasicNetwork network;
    private Muse muse = null;
    private ConnectionListener connectionListener = null;
    private DataListener dataListener = null;
    private boolean dataTransmission = true;

    Bluetooth_Blend_Interface BTAI; // BlueTooth Arduino Interface (BTAI) object
    boolean enable_BT_arm = true;
    boolean isBTConnected = false;
    Button mConnectBtn;

    public void setBTConnected(boolean toSet){
        isBTConnected = toSet;
    }

    public MainActivity() {
        // Create listeners and pass reference to activity to them
        WeakReference<Activity> weakActivity =
                new WeakReference<Activity>(this);
        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);
        Button connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);
        Button disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(this);
        Button pauseButton = (Button) findViewById(R.id.pause);
        pauseButton.setOnClickListener(this);
        Log.i("Muse Headband", "libmuse version=" + LibMuseVersion.SDK_VERSION);

        try {
            network = (BasicNetwork) EncogDirectoryPersistence.loadObject(getAssets().open("network"));
            System.out.println("ok");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (enable_BT_arm) {
            BTAI = new Bluetooth_Blend_Interface(this, enable_BT_arm);
            registerReceiver(BTAI.get_mGattUpdateReceiver(), Bluetooth_Blend_Interface.makeGattUpdateIntentFilter());
        }
        mConnectBtn = (Button)findViewById(R.id.connect_btn);
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
    public void onClick(View v) {
        Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
        if (v.getId() == R.id.refresh) {
            MuseManager.refreshPairedMuses();
            List<Muse> pairedMuses = MuseManager.getPairedMuses();
            List<String> spinnerItems = new ArrayList<String>();
            for (Muse m: pairedMuses) {
                String dev_id = m.getName() + "-" + m.getMacAddress();
                Log.i("Muse Headband", dev_id);
                spinnerItems.add(dev_id);
            }
            ArrayAdapter<String> adapterArray = new ArrayAdapter<String> (
                    this, android.R.layout.simple_spinner_item, spinnerItems);
            musesSpinner.setAdapter(adapterArray);
        }
        else if (v.getId() == R.id.connect) {
            List<Muse> pairedMuses = MuseManager.getPairedMuses();
            if (pairedMuses.size() < 1 ||
                    musesSpinner.getAdapter().getCount() < 1) {
                Log.w("Muse Headband", "There is nothing to connect to");
            }
            else {
                muse = pairedMuses.get(musesSpinner.getSelectedItemPosition());
                ConnectionState state = muse.getConnectionState();
                if (state == ConnectionState.CONNECTED ||
                        state == ConnectionState.CONNECTING) {
                    Log.w("Muse Headband", "doesn't make sense to connect second time to the same muse");
                    return;
                }
                configure_library();
                /**
                 * In most cases libmuse native library takes care about
                 * exceptions and recovery mechanism, but native code still
                 * may throw in some unexpected situations (like bad bluetooth
                 * connection). Print all exceptions here.
                 */
                try {
                    muse.runAsynchronously();
                } catch (Exception e) {
                    Log.e("Muse Headband", e.toString());
                }
            }
        }
        else if (v.getId() == R.id.disconnect) {
            if (muse != null) {
                /**
                 * true flag will force libmuse to unregister all listeners,
                 * BUT AFTER disconnecting and sending disconnection event.
                 * If you don't want to receive disconnection event (for ex.
                 * you call disconnect when application is closed), then
                 * unregister listeners first and then call disconnect:
                 * muse.unregisterAllListeners();
                 * muse.disconnect(false);
                 */
                muse.disconnect(true);
            }
        }
        else if (v.getId() == R.id.pause) {
            dataTransmission = !dataTransmission;
            if (muse != null) {
                muse.enableDataTransmission(dataTransmission);
            }
        }
    }

    private void configure_library() {
        muse.registerConnectionListener(connectionListener);
        muse.registerDataListener(dataListener,
                MuseDataPacketType.ALPHA_ABSOLUTE);
        muse.registerDataListener(dataListener,
                MuseDataPacketType.BETA_ABSOLUTE);
        muse.registerDataListener(dataListener,
                MuseDataPacketType.GAMMA_ABSOLUTE);
        muse.registerDataListener(dataListener,
                MuseDataPacketType.THETA_ABSOLUTE);
        muse.setPreset(MusePreset.PRESET_14);
        muse.enableDataTransmission(dataTransmission);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
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
}
