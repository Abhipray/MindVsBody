package bluetooth_utilities;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.brainvsmuscle.brainvsmuscle.MainActivity;

public class Bluetooth_Blend_Interface {

	Boolean enabled;
	public BluetoothAdapter btAdapter = null;
	public BluetoothSocket btSocket = null;
	public OutputStream outStream = null;	

	MainActivity act;
	public static final String TAG = "Arduino Bluetooth Interface";

	private BluetoothGattCharacteristic characteristicTx = null;
	private RBLService mBluetoothLeService = null;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mDevice = null;
	private String mDeviceAddress;

	private boolean flag = true;

	private boolean scanFlag = false;
	
	private byte[] data = new byte[3];
	private static final int REQUEST_ENABLE_BT = 1;
	private static final long SCAN_PERIOD = 2000;
	
	final private static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6',
		'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	

	private boolean connState = false;
	
	public BluetoothAdapter getBluetoothAdapater()
	{
		return mBluetoothAdapter;
	}
	public final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((RBLService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				act.finish();
			}
			connect();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};
	
	public void unbind_mServiceConnection(){
		if (mServiceConnection != null)
			act.unbindService(mServiceConnection);
	}
	
	private void startReadRssi() {
		new Thread() {
			public void run() {

				while (flag) {
					mBluetoothLeService.readRssi();
					try {
						sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}
	
	private void getGattService(BluetoothGattService gattService) {
		if (gattService == null)
			return;

		//startReadRssi();

		characteristicTx = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

		BluetoothGattCharacteristic characteristicRx = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
		mBluetoothLeService.setCharacteristicNotification(characteristicRx,
				true);
		mBluetoothLeService.readCharacteristic(characteristicRx);
	}
	
	public static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

		return intentFilter;
	}

	private void scanLeDevice() {
		new Thread() {

			@Override
			public void run() {
				mBluetoothAdapter.startLeScan(mLeScanCallback);

				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			}
		}.start();
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				final byte[] scanRecord) {

			act.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					byte[] serviceUuidBytes = new byte[16];
					String serviceUuid = "";
					for (int i = 32, j = 0; i >= 17; i--, j++) {
						serviceUuidBytes[j] = scanRecord[i];
					}
					serviceUuid = bytesToHex(serviceUuidBytes);
					if (stringToUuidString(serviceUuid).equals(
							RBLGattAttributes.BLE_SHIELD_SERVICE
									.toUpperCase(Locale.ENGLISH))) {
						mDevice = device;
					}
				}
			});
		}
	};

	public void connect()
	{
		scanLeDevice();

		Timer mTimer = new Timer();
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (mDevice != null) {
					mDeviceAddress = mDevice.getAddress();
					mBluetoothLeService.connect(mDeviceAddress);
					scanFlag = true;
					connState = true;
				} else {
					act.runOnUiThread(new Runnable() {
						public void run() {
							Toast toast = Toast
									.makeText(
											act,
											"Couldn't find BLE device!",
											Toast.LENGTH_SHORT);
							toast.setGravity(0, 0, Gravity.CENTER);
							toast.show();
						}
					
					});
					connState = false;
				}
			}
		}, SCAN_PERIOD);

		System.out.println(connState);
		if (connState == false) {
			mBluetoothLeService.connect(mDeviceAddress);
			connState = true;
		} else {
			mBluetoothLeService.disconnect();
			mBluetoothLeService.close();
			connState = false;
		}
	}
	
	public void disconnect(){
		if(mBluetoothLeService != null){
			mBluetoothLeService.disconnect();
			mBluetoothLeService.close();
			connState = false;
			act.setBTConnected(false);
			user_disconnect = true;
		}
		
	}
	public boolean isConnected(){
		return connState;
	}
	private String stringToUuidString(String uuid) {
		StringBuffer newString = new StringBuffer();
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
		newString.append("-");
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
		newString.append("-");
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
		newString.append("-");
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
		newString.append("-");
		newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

		return newString.toString();
	}
	
	private String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	//Here's a runnable/handler combo
	private Runnable mMyRunnable = new Runnable()
	{
	    @Override
	    public void run()
	    {
	    	connect();
	    }
	 };
	 
	private boolean user_disconnect;
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
				Toast.makeText(act, "Disconnected",
						Toast.LENGTH_SHORT).show();
				act.setBTConnected(false);
				if(!user_disconnect){
					Handler myHandler = new Handler();
					myHandler.postDelayed(mMyRunnable, 300);//Message will be delivered in 1 second.
				}
				else{
					user_disconnect = false;
				}
			} else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				Toast.makeText(act, "Connected",
						Toast.LENGTH_SHORT).show();
				getGattService(mBluetoothLeService.getSupportedGattService());

				act.setBTConnected(true);
			} else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
				data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);

			} else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
				//displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
			}
		}
	};
	
	public BroadcastReceiver get_mGattUpdateReceiver()
	{
		return this.mGattUpdateReceiver;
	}
	
	public Bluetooth_Blend_Interface(MainActivity a, Boolean en){
		enabled = en;
		act = a;
	    if(enabled){
	    	final BluetoothManager mBluetoothManager = (BluetoothManager) act.getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = mBluetoothManager.getAdapter();
			if (mBluetoothAdapter == null) {
				Toast.makeText(act, "Ble not supported", Toast.LENGTH_SHORT)
						.show();
				act.finish();
				return;
			}
	
			Intent gattServiceIntent = new Intent(act,
					RBLService.class);
			act.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
			checkBTState();
	    }		
	    //connect();
	}
	
	public void init_duinos(){
		if(enabled){
			byte[] initcode = new byte[1];
			initcode[0] = 0x3;
			sendData(initcode);
		}
	}
	
	public void turn_off(){
		//BTonPause();
		enabled = false;
		//unregisterReceiver(mGattUpdateReceiver);
	}
	
	public void turn_on(){
		enabled = true;
	    btAdapter = BluetoothAdapter.getDefaultAdapter();
		checkBTState();
		//BTBoot();
	}
	
 	
	  public void checkBTState() {
		    if(mBluetoothAdapter==null) { 
		    	BTerrorExit("Fatal Error", "Bluetooth not support");
		    } else {
		      if (mBluetoothAdapter.isEnabled()) {
		        Log.d(TAG, "...Bluetooth ON...");
		      } else {
		        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		        act.startActivityForResult(enableBtIntent, 1);
		      }
		    }
		  }		  
	  
	  public void sendData(byte[] msgBuffer) {
//		  int[] test_frame = new int[24];
//		  for(int i = 0; i < 24; i++){
//			  test_frame[i] = 7;
//		  }
//		  byte[] tbframe = new byte[CODEC_System.Utilities.app_get_bytes_per_protocol_frame_1(3, 24)];
//		  tbframe = CODEC_System.Utilities.protocol_encode_1(test_frame, 3);
		  if(connState && (characteristicTx != null))
		  {
			  byte[] buf = new byte[8];
	          buf[0] = 0x01;
	          String dbg_str ="";
	          for(int i =0;i<msgBuffer.length;i++)
	          {
	        	  dbg_str += Integer.toHexString(msgBuffer[i])+" ";
	          }
	          Log.e("ABHI", dbg_str);
			  characteristicTx.setValue(msgBuffer);
	          mBluetoothLeService.writeCharacteristic(characteristicTx);
		  }
		}		  
	  
	  public void BTerrorExit(String title, String message){
		//  Toast.makeText(parent.getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();		  
		  act.finish();
	  }
	  
	  public void BTonPause(){
		    Log.d(TAG, "...In onPause()...");
		    if (outStream != null) {
		      try {
		        outStream.flush();
		      } catch (IOException e) {
		        BTerrorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
		      }
		    }
		 
		    try     {
		      btSocket.close();
		    } catch (IOException e2) {
		      BTerrorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
		    }	  
	  } 
	  
}
