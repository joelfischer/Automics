package mrl.automics.sensors;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

public class SensorManager_Service extends Service {
	private final static String TAG = "SensorManager_Service";
	private static final int NEW_DIST = 1;
	private static final int DELAY_10 = 10*1000;	//constants for lag between cloud pull
	private static final int DELAY_15 = 15*1000;
	private static final int DELAY_30 = 30*1000;
	private static final int DELAY_60 = 60*1000;
	private boolean locIsBound;
//	private LocListener_Service locBoundService;
	private IRemoteService locBoundService;
	private PowerManager pm;
	private WakeLock wl;
	private RunLocationSensor locationThread;
	private RunCloudPull cloudThread;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		pm = (PowerManager)getSystemService(POWER_SERVICE);

				
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wl.acquire();
		
		locationThread = new RunLocationSensor(); 
		new Thread(locationThread).start();
		cloudThread = new RunCloudPull();
		new Thread(cloudThread).start();
	}
	
	@Override 
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		if (locIsBound)
			unbindService(locConnection);
		if (wl.isHeld())
			wl.release();
		if (mHandler != null && locationThread != null)
			mHandler.removeCallbacks(locationThread);
		if (mHandler != null && cloudThread != null)
			mHandler.removeCallbacks(cloudThread);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		//who binds to this Service?
		return null;
	}
	
	public class RunLocationSensor implements Runnable {
		RunLocationSensor() {}
		public void run() {
			bindService(new Intent(SensorManager_Service.this,LocListener_Service.class), locConnection, Context.BIND_AUTO_CREATE);
		}
	}
	
	public class RunCloudPull implements Runnable {
		RunCloudPull() {}
		public void run() {
			//only run the download service if we're not still downloading anything...
			if (!CloudPull_Service.activeThread) 
				startService(new Intent(SensorManager_Service.this, CloudPull_Service.class));
			mHandler.postDelayed(this, DELAY_10);
//			Log.d(TAG, "ok, scheduled to check cloud again in 60s");
		}
	}
	
	
	
	private ServiceConnection locConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			locBoundService = IRemoteService.Stub.asInterface(service);
			locIsBound = true;
			Log.d(TAG, "locIsBound onServiceConnected: "+locIsBound);
			try {
                locBoundService.registerCallback(mCallback);
            } catch (RemoteException e) {
            
            }

		}

		public void onServiceDisconnected(ComponentName className) {
			locBoundService = null;
			locIsBound = false;
			Log.d(TAG, "LocListener_Service disconnected");
			
		}
	};
	
	private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
        	
            switch (msg.what) {
                case NEW_DIST:                        
                    //get distance to nearest POI from Service
                    String dist = (String)msg.obj;
                    Log.d(TAG ,"from LocListener_Service: "+dist);
                    double distance = Double.parseDouble(dist);
                    evaluateDistance(distance);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };
	
	private IRemoteServiceCallback mCallback = new IRemoteServiceCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a locationThread
         * pool running in each process, so the code executing here will
         * NOT be running in our main locationThread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        public void valueChanged(String value) {
            mHandler.sendMessage(mHandler.obtainMessage(NEW_DIST, value));
        }
    };
	
	private void evaluateDistance(double minDistanceToPoi) {
		//do sth
		Log.d(TAG, "minDistanceToPoi: "+minDistanceToPoi);
		if (minDistanceToPoi <= 50) {
			//keep LocListener_Service running, i.e. do nothing here
			//TODO: Really...?, what if I've already fired?
			Log.d(TAG, "ok, keep running");
//			Toast.makeText(SensorManager_Service.this, "closer than 50m to POI, keep checking..." , Toast.LENGTH_SHORT).show();

		}
		if (minDistanceToPoi <= 100 && minDistanceToPoi > 50) {
			//schedule to run again in 15s
			unbindService(locConnection);
			locIsBound = false;
			locationThread = new RunLocationSensor();
			mHandler.postDelayed(locationThread, DELAY_15);
			Log.d(TAG, "ok, scheduled to run again in 15s");
//			Toast.makeText(SensorManager_Service.this, "closer than 100m to POI, check again in 15s" , Toast.LENGTH_SHORT).show();

		}
		if (minDistanceToPoi > 100 & minDistanceToPoi <= 200) {
			unbindService(locConnection);
			locIsBound = false;
			//schedule to run again in 30 sec. 
			locationThread = new RunLocationSensor();
			mHandler.postDelayed(locationThread, DELAY_30);
			Log.d(TAG, "ok, scheduled to run again in 30s");
//			Toast.makeText(SensorManager_Service.this, "smallest distance to POI is 100-200m, check again in 30s" , Toast.LENGTH_SHORT).show();
		}
		
		if (minDistanceToPoi > 200) {
			unbindService(locConnection);
			locIsBound = false;
			//schedule to run again in 1 min.
			locationThread = new RunLocationSensor();
			mHandler.postDelayed(locationThread, DELAY_60);
			Log.d(TAG, "ok, scheduled to run again in 60s");
			Toast.makeText(SensorManager_Service.this, "smallest distance to POI > 200m, check again in 1min" , Toast.LENGTH_LONG).show();
		}
	}
	

}
