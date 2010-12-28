package mrl.automics.sensors;

import java.util.ArrayList;
import java.util.Arrays;

import mrl.automics.R;
import mrl.automics.sensors.IRemoteService;
import mrl.automics.sensors.IRemoteServiceCallback;
import mrl.automics.storage.LocationModelSQLHelper;
import mrl.automics.storage.LocationsSQLHelper;
import mrl.automics.storage.TriggerHistorySQLHelper;
import mrl.automics.ui.ExperienceStarter_Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class LocListener_Service extends Service {
	
	private final static String TAG = "LocListener_Service";
	private final static long LISTENER_INTERVAL = 3*1000; //in milliseconds
	private final static float MIN_DISTANCE = 0;
	private final static double TEST_LAT = 52.953453;
	private final static double TEST_LONG = -1.186911;
	private final static double HOME_LAT = 52.957509;
	private final static double HOME_LONG = -1.196365;
	private final static double CYCLE_LAT = 52.952211;
	private final static double CYCLE_LONG = -1.186247;
	private final static String PROX_ALERT = "mrl.automics.sensors.PROX_ALERT";
	private final static float RADIUS = 100;
	private final static float RADIUS_SMALL = 20;
	private final static int REC_DIST = 20;
	private NotificationManager mNM;
	private WorkerThread thread;
	private LocationManager locMgr;
	private MyLocationListener locListener;
	private float distance;
	private SQLiteDatabase db;
	private SQLiteDatabase locationhistory;
	private ArrayList<GPSTrigger> gpsTriggers;
	
	public class LocalBinder extends Binder {
		LocListener_Service getService() {
			return LocListener_Service.this;
		}
		public float getDistance() {
			return distance;
		}
	}
	
	
	@Override
    public void onCreate() {
		Log.d(TAG, "onCreate()");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        locMgr = (LocationManager)getSystemService(LOCATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
        
        db = new LocationModelSQLHelper(LocListener_Service.this).getReadableDatabase();
        locationhistory = new LocationsSQLHelper(this).getWritableDatabase();

    }

	
	
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		Log.i("LocalService", "Received start id " + startId + ": " + intent);
//        
//		//run the thread that does the work
//        thread = new WorkerThread();
//        thread.run();
//		
//		return startId;
//	}
	
	private static final int REPORT_MSG = 1;
    
    /**
     * Our Handler used to execute operations on the main thread.  
     */
    private final Handler mHandler = new Handler() {
        @Override 
        public void handleMessage(Message msg) {
            switch (msg.what) {
                
                case REPORT_MSG: {
                	
                	Log.d(TAG, "(String)msg.obj:" +(String)msg.obj);
                	
                	// Broadcast to all clients the new value.
                    final int N = mCallbacks.beginBroadcast();
                    for (int i=0; i<N; i++) {
                        try {
                            mCallbacks.getBroadcastItem(i).valueChanged((String)msg.obj);
                        } catch (RemoteException e) {
                            // The RemoteCallbackList will take care of removing
                            // the dead object for us.
                        }
                    }
                    mCallbacks.finishBroadcast();
                } 
                break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
            
	public class WorkerThread implements Runnable {
    	
    	public boolean isRunning = false;

     	WorkerThread() {}
     		
		public void run() {
			locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, LISTENER_INTERVAL, MIN_DISTANCE,	
	    			locListener = new MyLocationListener());
			this.isRunning = true;
		}
    }
	
	public class MyLocationListener implements LocationListener {
		
		Location last;
		Location current;
		
		public void onLocationChanged(Location location) {
			//debug
    		Log.d("LocListener_Service","onLocationChanged");
    		
//    		double lat = location.getLatitude();
//    		double lng = location.getLongitude();
//    		
//    		Log.d("LocListener_Service", "lat: "+ lat + " lng: "+lng);
//    		
//    		Toast.makeText(LocListener_Service.this, "got fix", Toast.LENGTH_SHORT).show();
    		
    		//get GPS triggers from location model
    		if (db == null || !db.isOpen() )
    	        db = new LocationModelSQLHelper(LocListener_Service.this).getReadableDatabase();
    		
    		if (gpsTriggers == null)
    			gpsTriggers = new ArrayList<GPSTrigger>();
    		
    		int tagId = 0;
    		String type = "";
    		int radius = 0;
    		String comment = "";
    		double lat = 0;
    		double lng = 0;
    		float[] triggerDistances = null;
    		
    		Location trigger = new Location(location);
    		
    		recordGpsTrace(location);
    		
    		//iterate over entries from location model
    		Cursor c = db.rawQuery("SELECT tagId,type,radius,comment,lat,long FROM gps_triggers", null);
    		if (c.getCount() > 0) {
    			triggerDistances = new float[c.getCount()];
    			//the first trigger needs to be checked separately
    			c.moveToFirst();
    			try {
					tagId = c.getInt(0);
					type = c.getString(1);
					radius = c.getInt(2);
					comment = c.getString(3);
					lat = c.getDouble(4);
					lng = c.getDouble(5);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
				trigger.setLatitude(lat);
				trigger.setLongitude(lng);
							
				GPSTrigger gpsTr;
				//as this this the first trigger, the ArrayList holding the triggers will either be 
				//empty or it will have the trigger already set at the first position (=c.getPosition())
				if (gpsTriggers.isEmpty()) {
					gpsTr = new GPSTrigger(tagId, type, radius, comment, lat, lng);
					gpsTriggers.add(c.getPosition(), gpsTr);
				}
				else 
					gpsTr = gpsTriggers.get(c.getPosition());
				
				//get the distance from current location to this gps trigger from location model
				distance = location.distanceTo(trigger);
//				Log.d(TAG, "distance: "+distance + " type: "+type+", hasFired: "+gpsTr.fired);
				triggerDistances [c.getPosition()] = distance;
				
				//if this trigger hasn't fired yet or needs to be kept checking, evaluate the distance to the current location
				if (!gpsTr.fired || gpsTr.keepChecking)
					evaluateDistance(distance, radius, type, comment, gpsTr);
				
				//iterate over the rest of the triggers
    			while (c.moveToNext()) {
    				try {
    					tagId = c.getInt(0);
    					type = c.getString(1);
    					radius = c.getInt(2);
    					comment = c.getString(3);
    					lat = c.getDouble(4);
    					lng = c.getDouble(5);
    				} catch (Exception e) {
    					Log.e(TAG, e.toString());
    				}
    				trigger.setLatitude(lat);
    				trigger.setLongitude(lng);
    				    				
    				try {
    					gpsTr = gpsTriggers.get(c.getPosition());
    				} catch (Exception e) {
    					Log.e(TAG, e.toString());
    					//is empty, create
    					gpsTr = new GPSTrigger(tagId, type, radius, comment, lat, lng);
    					gpsTriggers.add(c.getPosition(), gpsTr);
    				}
    		
    				
    				distance = location.distanceTo(trigger);
    				//debug
//    				Log.d(TAG, "distance: "+distance + " type: "+type+", hasFired: "+gpsTr.fired);
    				triggerDistances [c.getPosition()] = distance;
    				
    				if (!gpsTr.fired || gpsTr.keepChecking)
    					evaluateDistance(distance, radius, type, comment, gpsTr);
    			}
    			//sort so that triggerDistances[0] will be the smallest current distance, 
    			//which is to be reported back to the SensorManager_Service
    			Arrays.sort(triggerDistances);
    			
    			//debug
//    			for (int i = 0; i < triggerDistances.length; i++) 
//    				Log.d(TAG, "distance at "+i +": "+triggerDistances[i]);
//        		Log.d(TAG, "smallest distance: "+triggerDistances[0]);

    		}
    		c.close();
    		
    		//debug stuff with testlocations
//    		Location poi2 = new Location(location);
//    		Location home = new Location(location);
//    		poi.setLatitude(TEST_LAT);
//    		poi.setLongitude(TEST_LONG);
//    		poi2.setLatitude(CYCLE_LAT);
//    		poi2.setLongitude(CYCLE_LONG);
//    		home.setLatitude(HOME_LAT);
//    		home.setLongitude(HOME_LONG);
//     		
//    		float distance1 = location.distanceTo(poi);
//    		float distance2 = location.distanceTo(poi2);
//    		float distanceHome = location.distanceTo(home);
//    		
//    		Log.d(TAG, "distance1: "+ distance1);
//    		Log.d(TAG, "distance2: "+ distance2);
//    		
//    		if (distance1 <= RADIUS_SMALL) {
//    			LocListener_Service.this.sendBroadcast(new Intent(PROX_ALERT).putExtra("POI", "CS street 1"));
//    		}
//    		if (distance2 <= RADIUS_SMALL) {
//    			LocListener_Service.this.sendBroadcast(new Intent(PROX_ALERT).putExtra("POI", "cyclepath"));
//    			Toast.makeText(LocListener_Service.this, "You're within 20m of POI", Toast.LENGTH_LONG).show();
//    		}
//    		if (distanceHome <= RADIUS_SMALL) {
//    			LocListener_Service.this.sendBroadcast(new Intent(PROX_ALERT).putExtra("POI", "home"));
//    			Toast.makeText(LocListener_Service.this, "You're within 20m of Home", Toast.LENGTH_LONG).show();
//    		}
//    		
//    		//set smallest distance as value to send back to calling client
//    		float distanceTemp = distance1 < distance2 ? distance1 : distance2;	//ternary operator. read: if d1 < d2 then assign dist1 to distance, else assign dist2 to distance
//    		distance = distanceTemp < distanceHome ? distanceTemp : distanceHome;
//			Log.d(TAG, "smallest distance to be reported: "+distance);
			
    		//report back to the SensorManager_Service the smallest distance...
			mHandler.sendMessage(Message.obtain(mHandler, REPORT_MSG, ""+triggerDistances[0]));
		}
		
		private void evaluateDistance(float dist, int radius, String type, String comment, GPSTrigger trigger) {
			
			Log.d(TAG, "dist: "+dist+", rad: "+radius+", type: "+type +", comment: "+comment + "id: "+trigger.id);
			
			if (dist <= radius) {
						
				Intent proxAlert = new Intent(PROX_ALERT);
				proxAlert.putExtra("comment", comment);
				proxAlert.putExtra("type", type);
				proxAlert.putExtra("radius", radius);
				proxAlert.putExtra("id", trigger.id);
    			LocListener_Service.this.sendBroadcast(proxAlert);
    			trigger.hasFired(true);
    			//in case this trigger is of the type "lunch/break", we need to set this flag so we keep checking it until
    			//an opportune moment arises
    			if (type.equals("lunch/break")) {
    				trigger.keepChecking = true;
    			}
    				
				if (!type.equals("lunch/break")) {
	    			//do nada
				}
    			
    			//debug
//    			Toast.makeText(LocListener_Service.this, "You're within "+radius+"m of a "+type, Toast.LENGTH_LONG).show();
    		}
		}
		
		private void recordGpsTrace(Location current) {
			
			LastLocation.lat = current.getLatitude();
			LastLocation.lng = current.getLongitude();
						
			if (last==null) {
				last = current;
				if (locationhistory==null || !locationhistory.isOpen())
					locationhistory = new LocationsSQLHelper(LocListener_Service.this).getWritableDatabase();
				locationhistory.execSQL("INSERT INTO locations (lat,long,distance,timestamp) VALUES (" +
						current.getLatitude()+","+
						current.getLongitude()+","+
						0+"," +
						System.currentTimeMillis() +
						")"
						);
			}
			
			else if (last!=null) {
				float dist = last.distanceTo(current);
				
				if (dist>REC_DIST) {
					if (locationhistory==null || !locationhistory.isOpen())
						locationhistory = new LocationsSQLHelper(LocListener_Service.this).getWritableDatabase();
					locationhistory.execSQL("INSERT INTO locations (lat,long,distance,timestamp) VALUES (" +
							current.getLatitude()*1E6 +","+
							current.getLongitude()*1E6 +","+
							dist+"," +
							System.currentTimeMillis() +
							")"
							);
					last = current;
				}
				
			}
		}

		public void onProviderDisabled(String provider) {
			Log.d(TAG, "provider disabled");
			
		}

		public void onProviderEnabled(String provider) {
			Log.d(TAG, "provider enabled");
			
		}

		public void onStatusChanged(String provider, int status,
				Bundle extras) {
			Log.d(TAG, "onStatusChanged");
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		
		//run the thread that does the work
        thread = new WorkerThread();
        thread.run();
		
		return mBinder;
	}
	
	private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {

		public void registerCallback(IRemoteServiceCallback cb)
				throws RemoteException {
			if (cb != null) mCallbacks.register(cb);
		}

		public void unregisterCallback(IRemoteServiceCallback cb)
				throws RemoteException {
			if (cb != null) mCallbacks.unregister(cb);
		}
		
	};
	
	@Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.loclistener_service_started);

        // Tell the user we stopped.
        Toast.makeText(this, "Location listener stopped", Toast.LENGTH_SHORT).show();
        
        if (locMgr != null && locListener != null)
        	locMgr.removeUpdates(locListener);
        
        if (db!=null || db.isOpen())
        	db.close();
        
        if (locationhistory!=null || locationhistory.isOpen()) 
        	locationhistory.close();
        
//        locMgr.removeProximityAlert(PendingIntent.getBroadcast(this, 0, new Intent(PROX_ALERT), 0));
    }
	
	private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.loclistener_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.stat_sample, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, ExperienceStarter_Activity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.loclistener_service_label),
                       text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.loclistener_service_started, notification);
    }

	final RemoteCallbackList<IRemoteServiceCallback> mCallbacks
    = new RemoteCallbackList<IRemoteServiceCallback>();

}
